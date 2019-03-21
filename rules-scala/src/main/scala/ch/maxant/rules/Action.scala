package ch.maxant.rules

class Action[I,O](name: String)(f: I => O) extends AbstractAction[I,O](name) {

    def execute(input: I) = {
        f(input)
    }
}