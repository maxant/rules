package ch.maxant.rules;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.script.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A Javascript based Subclass of {@link Engine}. <br>
 * <b>ONLY TESTED WITH JAVA 8 AND NASHORN!!</b><br>
 * Quite likely to work very differently with Java SE 6 or Java SE 7!
 */
public class JavascriptEngine extends Engine {

	private static final Logger log = Logger.getLogger(JavascriptEngine.class.getName());

	private static final String MIME_TYPE = "text/javascript";

	private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();
	
	static {
		ScriptEngine engine = ENGINE_MANAGER.getEngineByMimeType(MIME_TYPE);
		ScriptEngineFactory factory = engine.getFactory();
		if(log.isLoggable(Level.INFO)){
            log.info("Using JavaScript engine " + factory.getEngineName() + "/"
                + factory.getEngineVersion() + "/"
                + factory.getLanguageName() + "/"
                + factory.getLanguageVersion() + "/"
                + factory.getExtensions() + "/"
                + factory.getMimeTypes() + "/"
                + factory.getNames() + "/"
                + "threading model: " + factory.getParameter("THREADING")
            );
        }
	}

	private final class PoolableEngineFactory extends BasePooledObjectFactory<Engine> {
		@Override
		public Engine create() throws Exception {
			log.info("\r\n\r\n>>>Creating JavaScript rule engine...<<<");
			long start = System.currentTimeMillis();
			Engine engine = new Engine();
			engine.engine = ENGINE_MANAGER.getEngineByMimeType(MIME_TYPE);
			compile(engine);
			log.info(">>>JavaScript rule engine initialisation completed in " + (System.currentTimeMillis()-start) + " ms<<<\r\n");
			return engine;
		}
		@Override
		public PooledObject<Engine> wrap(Engine obj) {
			return new DefaultPooledObject<Engine>(obj);
		}
	}
	
	/** since {@link CompiledScript} depends on the engine which compiled it,
	 * we cant just map a {@link Rule} to {@link CompiledScript}, as is done with MVEL.
	 * Instead, we need to take an engine out of the pool, and use its compiled scripts.
	 * this class encapsulates that.
	 */
	private static final class Engine {
		private ScriptEngine engine;
		private Map<Rule, CompiledScript>  rules = new HashMap<Rule, CompiledScript>();
	}

	/**
	 * Why are we pooling engines?  Nashorn isn't thread-safe:<br>
	 * https://blogs.oracle.com/nashorn/entry/nashorn_multi_threading_and_mt<br>
	 * http://mail.openjdk.java.net/pipermail/nashorn-dev/2013-July/001567.htm<br>
	 * http://stackoverflow.com/questions/27710407/reuse-nashorn-scriptengine-in-servlet<br>
	 */
	private ObjectPool<Engine> engines;

	/**
	 * @return [numActive, numIdle] 
	 */
	public int[] getPoolSize(){
		return new int[]{engines.getNumActive(), engines.getNumIdle()};
	}
	
	/**
	 * Creates the engine with a pool size of {@value GenericObjectPoolConfig#DEFAULT_MAX_TOTAL}. 
	 * The pool is not preloaded.
	 * @param rules The rules which define the system. Please note that rules may access the input using 
	 * 			bean notation (e.g. "<code>input.people[0].name</code>") OR 
	 * 			Java notation (e.g. "<code>input.getPeople().get(0).getName()</code>").
	 * @param throwExceptionIfCompilationFails if true, and a rule cannot be compiled, then a {@link CompileException} will be thrown.
	 * @param javascriptFilesToLoad optional list of scripts to load - either script names found on classpath, or actual scripts.
	 * @throws DuplicateNameException thrown if any rules have the same name within a namespace
	 * @throws CompileException thrown if throwExceptionIfCompilationFails is true, and a rule fails to compile, because its expression is invalid
	 * @throws ParseException Thrown if a subrule which is referenced in a rule cannot be resolved.
	 */
	public JavascriptEngine(final Collection<Rule> rules, boolean throwExceptionIfCompilationFails, String... javascriptFilesToLoad) throws DuplicateNameException, CompileException, ParseException {
		this(rules, DEFAULT_INPUT_NAME, throwExceptionIfCompilationFails, null, false, javascriptFilesToLoad);
	}

	/**
	 * See {@link #JavascriptEngine(Collection, boolean, String...)
	 * @param inputName the name of the input in scripts, normally "input", but you can specify your own name here.
	 * @param poolSize the maximum size of the pool. You can override more of the pool configuration by overriding the method {@link #getPoolConfig()}.
	 * @param preloadPool if true, then before the constructor returns, it fills the pool.
	 */
	public JavascriptEngine(final Collection<Rule> rules, String inputName, boolean throwExceptionIfCompilationFails, Integer poolSize, boolean preloadPool, String... javascriptFilesToLoad) throws DuplicateNameException, CompileException, ParseException {
	    this(rules, inputName, throwExceptionIfCompilationFails, poolSize, preloadPool, new HashMap<String, Object>(), javascriptFilesToLoad);
    }

	/**
	 * See {@link #JavascriptEngine(Collection, boolean, String...)
     * <br><br>
     * Allows you to define constants or static methods which rules can refer to.
     * <code>
     * Map<String, Object> statics = new HashMap<String, Object>();
     * statics.put("someString", "this is a constant");
     * Rule rule1 = new Rule("1", "input.name == someString", "ok", 1, "ch.maxant.demo");
     * </code>
     *
	 * @param inputName the name of the input in scripts, normally "input", but you can specify your own name here.
	 * @param poolSize the maximum size of the pool. You can override more of the pool configuration by overriding the method {@link #getPoolConfig()}.
	 * @param preloadPool if true, then before the constructor returns, it fills the pool.
     * @param statics a map containing variable bindings which do not change, e.g. constants or static methods (functions).
     *
	 */
	public JavascriptEngine(final Collection<Rule> rules, String inputName, boolean throwExceptionIfCompilationFails, Integer poolSize, boolean preloadPool, Map<String, Object> statics, String... javascriptFilesToLoad) throws DuplicateNameException, CompileException, ParseException {
		super(rules, inputName, throwExceptionIfCompilationFails, poolSize, javascriptFilesToLoad, statics);
		
		if(preloadPool){
			
			try {
				List<Engine> borrowed = new ArrayList<JavascriptEngine.Engine>();
				for(int i = 0; i < (poolSize == null ? GenericObjectPoolConfig.DEFAULT_MAX_TOTAL : poolSize); i++){
					borrowed.add(engines.borrowObject());
				}
				for(Engine e : borrowed){
						engines.returnObject(e);
				}
			} catch (Exception e) {
				handlePoolProblem(e);
			}
		}
	}
	
	/**
	 * Subclasses may override this. But the default will create a config which sets up the 
	 * pool using the size passed into the constructor, otherwise accessible as 
	 * <code>poolSize</code>.
	 */
	protected GenericObjectPoolConfig getPoolConfig(){
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		if(poolSize != null){
			config.setMaxTotal(poolSize);
		}
		return config;
	}
	
	private void preloadOtherScripts(Engine engine) throws CompileException {
		if(javascriptFilesToLoad != null){
			ClassLoader cl = getClass().getClassLoader();
			InputStream script = null;
			for (String js : javascriptFilesToLoad) {
				//fetch script file from classloader (e.g. out of a JAR) and put it into the engine
				boolean assumedItsAScriptNotAFile = false;
				try {
					//no need to compile, since we only load it once
					script = cl.getResourceAsStream(js);
					if(script == null){
						log.info("Assuming that the given string is an actual script, rather than the name of a file containing one: '" + js + "'");
						assumedItsAScriptNotAFile = true;
						engine.engine.eval(js);
					}else{
						log.info("Found script named '" + js + "' on classpath - attempting to evaluate it...");
						engine.engine.eval(new InputStreamReader(script));
					}
				} catch (ScriptException e) {
					if(assumedItsAScriptNotAFile){
						throw new CompileException("No file named '" + js + "' found on classpath. Assumed a script was passed instead.  But failed to evaluate script: " + e.getMessage());
					}else{
						throw new CompileException("Failed to evaluate script named '" + js + "': " + e.getMessage());
					}
				}finally{
					if(script != null){
						try {
							script.close();
						} catch (IOException e) {
							throw new RuntimeException(e); //should never happen
						}
					}
				}
			}
		}
	}

	private void returnEngineToPool(Engine engine) {
		if(engine != null){
			try {
				engines.returnObject(engine);
			} catch (Exception e) {
				handlePoolProblem(e);
			}
		}
	}

	private void handlePoolProblem(Exception e) {
		throw new RuntimeException("problem with engine pool", e); //should never happen
	}
	
	@Override
	protected void compile() throws CompileException {
		//this gets called by the constructor. 
		//it creates the very first engine.
		//no need to by synchronized, since this is called from the constructor
		Engine engine = null;
		try{
			if(engines == null){
				engines = new GenericObjectPool<Engine>(new PoolableEngineFactory(), getPoolConfig());
			}
			engine = engines.borrowObject();
		} catch (CompileException e) {
			throw e;
		} catch (Exception e) {
			handlePoolProblem(e);
		}finally{
			returnEngineToPool(engine);
		}
	}
	
	private void compile(Engine engine) throws CompileException {
		for(Rule r : parsedRules){
			try{
				if(r instanceof SubRule){
					continue;
				}
				CompiledScript compiledScript = ((Compilable)engine.engine).compile(r.getExpression());
				engine.rules.put(r, compiledScript);
			}catch(ScriptException ex){
				log.warning("Failed to compile " + r.getFullyQualifiedName() + ": " + ex.getMessage());
				if(throwExceptionIfCompilationFails){
					throw new CompileException(ex.getMessage());
				}
			}
		}
		preloadOtherScripts(engine);
	}
	
	@Override
	public <Input> List<Rule> getMatchingRules(String nameSpacePattern, Input input) {
		
		Pattern pattern = null;
		if(nameSpacePattern != null){
			pattern = Pattern.compile(nameSpacePattern);
		}

		Engine engine = null;
		Rule r = null;
		try {
			try {
				engine = engines.borrowObject();
			} catch (Exception e) {
				handlePoolProblem(e);
			}
			
			List<Rule> matchingRules = new ArrayList<Rule>();
			for(Entry<Rule, CompiledScript> e : engine.rules.entrySet()){
				r = e.getKey();
				if(pattern != null){
					if(!pattern.matcher(e.getKey().getNamespace()).matches()){
						continue;
					}
				}
			
				//execute
				engine.engine.getContext().setAttribute(inputName, input, ScriptContext.ENGINE_SCOPE);
				engine.engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE).putAll(this.statics);
				Object result = e.getValue().eval();
				String msg = r.getFullyQualifiedName() + "-{" + r.getExpression() + "}";
				if(String.valueOf(result).equals("true")){
					matchingRules.add(r);
					if(log.isLoggable(Level.INFO)) log.info("matched: " + msg);
				}else{
                    if(log.isLoggable(Level.INFO)) log.info("unmatched: " + msg);
				}
			}
			//order by priority!
			Collections.sort(matchingRules);
			
			return matchingRules;
		} catch (ScriptException e) {
			throw new IllegalArgumentException("Failed to run script " + r.getFullyQualifiedName(), e);
		}finally{
			returnEngineToPool(engine);
		}
	}

	public static final class Builder {
		
		private final Collection<Rule> rules;
		private String inputName = JavascriptEngine.DEFAULT_INPUT_NAME;
		private boolean throwExceptionIfCompilationFails = true;
		private Integer poolSize = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;
		private boolean preloadPool = false;
		private String[] javascriptFilesToLoad = {};

		public Builder(Collection<Rule> rules){
			this.rules = rules;
		}

		public Builder withInputName(String inputName){
			this.inputName = inputName;
			return this;
		}
		
		public Builder withThrowExceptionIfCompilationFails(boolean throwExceptionIfCompilationFails){
			this.throwExceptionIfCompilationFails = throwExceptionIfCompilationFails;
			return this;
		}
		
		public Builder withPoolSize(Integer poolSize){
			this.poolSize = poolSize;
			return this;
		}
		
		public Builder withPreloadPool(boolean preloadPool){
			this.preloadPool = preloadPool;
			return this;
		}
	
		public Builder withJavascriptFilesToLoad(String... javascriptFilesToLoad) {
			this.javascriptFilesToLoad = javascriptFilesToLoad;
			return this;
		}
		
		public JavascriptEngine build() throws DuplicateNameException, CompileException, ParseException {
			return new JavascriptEngine(rules, inputName, throwExceptionIfCompilationFails, poolSize, preloadPool, javascriptFilesToLoad);
		}
	}
}