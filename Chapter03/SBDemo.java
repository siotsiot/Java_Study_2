// Small BASIC 인터프리터 사용

class SBDemo {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SBDemo <filename>");
            return;
        }

        try {
            SBasic ob = new SBasic(args[0]);
            ob.run();
        }   catch (InterpreterException exc) {
            System.out.println(exc);
        }
    }
}
