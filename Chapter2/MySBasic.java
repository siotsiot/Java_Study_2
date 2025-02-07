// Small BASIC 인터프리터

import java.io.*;
import java.util.*;

// 인터프리터의 에러에 대한 예외 클래스
class InterpreterException extends Exception {
    String errStr; // 에러에 대한 설명

    public InterpreterException(String str) {
        errStr = str;
    }

    public String toString() {
        return errStr;
    }
}

// Small BASIC 인터프리터
class SBasic {
    final int PROG_SIZE = 10000; // 프로그램 크기의 최댓값

    // 토큰 타입
    final int NONE = 0;
    final int DELIMITER = 1;
    final int VARIABLE = 2;
    final int NUMBER = 3;
    final int COMMAND = 4;
    final int QUOTEDSTR = 5;

    // 에러 타입
    final int SYNTAX = 0;
    final int UNBALPARENS = 1;
    final int NOEXP = 2;
    final int DIVBYZERO = 3;
    final int EQUALEXPECTED = 4;
    final int NOTVAR = 5;
    final int LABELTABLEFULL = 6;
    final int DUPLABEL = 7;
    final int UNDEFLABEL = 8;
    final int THENEXPECTED = 9;
    final int TOEXPECTED = 10;
    final int NEXTWITHOUTFOR = 11;
    final int RETURNWITHOUTGOSUB = 12;
    final int MISSINGQUOTE = 13;
    final int FILENOTFOUND = 14;
    final int FILEIOERROR = 15;
    final int INPUTIOERROR = 16;

    // Small BASIC의 키워드에 대한 내부 표현
    final int UNKNCOM = 0;
    final int PRINT = 1;
    final int INPUT = 2;
    final int IF = 3;
    final int THEN = 4;
    final int FOR = 5;
    final int NEXT = 6;
    final int TO = 7;
    final int GOTO = 8;
    final int GOSUB = 9;
    final int RETURN = 10;
    final int END = 11;
    final int EOL = 12;
    final int REPEAT = 14;
    final int UNTIL = 15;

    // 프로그램의 끝을 의미하는 토큰
    final String EOP = "\0";

    // <=와 같은 이중 연산자에 대한 코드
    final char LE = 1;
    final char GE = 2;
    final char NE = 3;

    // 변수들을 위한 배열
    private double vars[];

    // 키워드들을 키워드 토큰과 연결시키는 클래스
    class Keyword {
        String keyword; // 문자열 형태
        int keywordTok; // 내부 표현

        Keyword(String str, int t) {
            keyword = str;
            keywordTok = t;
        }
    }

    /* 내부 표현을 포함한 키워드의 테이블.
       모든 키워드는 소문자로 저장 */
    Keyword kwTable[] = {
            new Keyword("print", PRINT),
            new Keyword("input", INPUT),
            new Keyword("if", IF),
            new Keyword("then", THEN),
            new Keyword("goto", GOTO),
            new Keyword("for", FOR),
            new Keyword("next", NEXT),
            new Keyword("to", TO),
            new Keyword("gosub", GOSUB),
            new Keyword("return", RETURN),
            new Keyword("end", END),
            new Keyword("repeat", REPEAT),
            new Keyword("until", UNTIL)
    };

    private char[] prog;    // 프로그램 배열을 참조
    private int progIdx;    // 프로그램의 위치에 대한 인덱스

    private String token;   // 현재의 토큰 저장
    private int tokType;    // 토큰의 타입 저장

    private int kwToken;    // 키워드의 내부 표현

    // FOR 루프 지원
    class ForInfo {
        int var;        // 카운터 변수
        double target;  // 목표 값
        int loc;        // 루프에 대한 소스 코드 내 인덱스
    }

    // FOR 루프를 위한 스택
    private Stack fStack;

    // 루프 상태 추적을 위한 클래스
    class RUInfo {
        int loc; // 루프 시작 위치
    }

    private Stack ruStack;

    // 레이블 테이블 항목들에 대한 정의
    class Label {
        String name;    // 레이블
        int loc;        // 소스 파일 내에서 레이블의 위치에 대한 인덱스

        public Label(String n, int i) {
            name = n;
            loc = i;
        }
    }

    // 레이블들에 대한 매핑
    private TreeMap labelTable;

    // gosub를 위한 스택
    private Stack gStack;

    // 관계 연산자
    char rops[] = {
            GE, NE, LE, '<', '>', '=', 0
    };

    /* 보다 편리하게 확인하기 위해
       관계 연산자를 포함하는 문자열을 생성 */
    String relops = new String(rops);

    // SBasic의 생성자
    public SBasic(String progName) throws InterpreterException {
        char tempbuf[] = new char[PROG_SIZE]; // 프로그램을 저장할 임시 변수 생성
        int size;

        // 실행하기 위해 프로그램을 메모리에 읽어들임
        size = loadProgram(tempbuf, progName);

        if (size != -1) {
            // 프로그램을 저장할 적당한 크기의 배열 생성
            prog = new char[size];

            // 프로그램을 프로그램 배열로 복사
            System.arraycopy(tempbuf, 0, prog, 0, size);
        }
    }

    // 프로그램을 메모리에 읽어들임
    private int loadProgram(char[] p, String fname) throws InterpreterException {
        int size = 0;
        try {
            /**
             * FileReader는 파일에엇 문자 단위로 데이터를 읽을 수 있게 도와주는 클래스
             * 파일의 경로를 매개변수로 받아 해당 파일을 열고, 읽기 준비
             */
            FileReader fr = new FileReader(fname);

            /**
             * 문자 기반 스트림을 효율적으로 읽는 기능
             * 버퍼를 사용해 여러 문자를 한 번에 읽음
             */
            BufferedReader br = new BufferedReader(fr);

            /**
             * p: 읽은 데이터를 저장할 배열
             * 0: 읽기 시작할 인덱스
             * PROG_SIZE: 읽을 최대 크기
             * size에는 읽은 데이터의 크기 저장
             */
            size = br.read(p, 0, PROG_SIZE);

            /**
             * 파일을 읽었으면 반드시 닫아야 함
             */
            fr.close();
        } catch (FileNotFoundException exc) { // 파일을 찾을 수 없는 경우
            handleErr(FILENOTFOUND); // "File not found" 출력
        } catch (IOException exc) { // 읽기 오류 발생
            handleErr(FILEIOERROR); // "I/O error while loading file" 출력
        }

        // 파일이 EOF 기호로 끝나는 경우, 크기를 1만큼 감소시킴
        /**
         * 파일이 끝(EOF; End Of File) 기호(char 값 26)로 끝나는 경우가 있음
         * 이는 특정 시스템에서 파일의 끝을 나타내는 특수 문자
         * 만약 존재하면, size를 1 감소시켜서 EOF 문자를 제외하고 파일의 실제 크기를 나타냄
         */
        if (p[size - 1] == (char) 26) size--;

        /**
         * 파일 크기 반환
         */
        return size;
    }

    // 프로그램 실행
    public void run() throws InterpreterException {
        // 새 프로그램 실행을 위한 초기화
        vars = new double[26];      // 변수 저장을 위한 배열
        fStack = new Stack();       // FOR 루프를 위한 스택
        labelTable = new TreeMap(); // 레이블을 저장하기 위한 트리맵
        gStack = new Stack();       // GOSUB를 위한 스택
        ruStack = new Stack();      // REPEAT/UNTIL을 위한 스택
        progIdx = 0;                // 파일 인덱스

        scanLabels(); // 프로그램 내에서 레이블을 검색

        sbInterp(); // 실행
    }

    // Small BASIC 인터프리터의 진입점
    private void sbInterp() throws InterpreterException {
        // 인터프리터의 메인 루프
        do {
            getToken();
            // 할당 구문에 대한 검사
            if (tokType == VARIABLE) {
                putBack();      // 입력 스트림에 var을 리턴
                assignment();   // 할당 구문을 처리
            } else // 키워드
                switch (kwToken) {
                    case PRINT:
                        print();
                        break;
                    case GOTO:
                        execGoto();
                        break;
                    case IF:
                        execIf();
                        break;
                    case FOR:
                        execFor();
                        break;
                    case NEXT:
                        next();
                        break;
                    case INPUT:
                        input();
                        break;
                    case GOSUB:
                        gosub();
                        break;
                    case RETURN:
                        greturn();
                        break;
                    case REPEAT:
                        repeat();  // REPEAT 구문 처리
                        break;
                    case UNTIL:
                        until();  // UNTIL 구문 처리
                        break;
                    case END:
                        return;
                }
        } while (!token.equals(EOP));
    }

    // 모든 레이블을 검색
    private void scanLabels() throws InterpreterException {
        int i;
        Object result;

        // 파일의 첫 번째 토큰이 레이블인지 검사
        getToken();
        if (tokType == NUMBER)
            labelTable.put(token, Integer.valueOf(progIdx));

        findEOL();

        do {
            getToken();
            if (tokType == NUMBER) { // 줄 번호
                result = labelTable.put(token, Integer.valueOf(progIdx));
                if (result != null)
                    handleErr(DUPLABEL);
            }

            // 공백 줄이 아니면 다음 줄 검색
            if (kwToken != EOL) findEOL();
        } while (!token.equals(EOP));
        progIdx = 0; // 프로그램의 시작점으로 인덱스를 재설정
    }

    // 다음 줄의 시작점을 검색
    private void findEOL() {
        while (progIdx < prog.length && prog[progIdx] != '\n') ++progIdx;
        if (progIdx < prog.length) progIdx++;
    }

    // 변수에 값을 할당
    private void assignment() throws InterpreterException {
        int var;
        double value;
        char vname;

        // 변수 이름을 얻음
        getToken();
        vname = token.charAt(0);

        if (!Character.isLetter(vname)) {
            handleErr(NOTVAR);
            return;
        }

        // 변수 테이블에 대한 인덱스로 변환
        var = (int) Character.toUpperCase(vname) - 'A';

        // 등호를 얻음
        getToken();
        if (!token.equals("=")) {
            handleErr(EQUALEXPECTED);
            return;
        }

        // 할당할 값을 얻은
        value = evaluate();

        // 값을 할당
        vars[var] = value;
    }

    // PRINT 구문의 간단한 버전을 실행
    private void print() throws InterpreterException {
        double result;
        int len = 0, spaces;
        String lastDelim = "";

        do {
            getToken(); // 다음 리스트 아이템을 얻음
            if (kwToken == EOL || token.equals(EOP)) break;

            if (tokType == QUOTEDSTR) { // 문자열
                System.out.print(token);
                len += token.length();
                getToken();
            } else { // 수식
                putBack();
                result = evaluate();
                getToken();
                System.out.print(result);

                // 현재의 합계에 결과 길이를 더함
                Double t = Double.valueOf(result);
                len += t.toString().length(); // 길이 저장
            }
            lastDelim = token;

            // 쉼표이면 다음 탭으로 이동
            if (lastDelim.equals(",")) {
                // 다음 탭으로 이동하기 위해 공백의 개수 계산
                spaces = 8 - (len % 8);
                len += spaces; // 탭 위치에 추가
                while (spaces != 0) {
                    System.out.print(" ");
                    spaces--;
                }
            } else if (token.equals(";")) {
                System.out.print(" ");
                len++;
            } else if (kwToken != EOL && !token.equals(EOP))
                handleErr(SYNTAX);
        } while (lastDelim.equals(";") || lastDelim.equals(","));

        if (kwToken == EOL || token.equals(EOP)) {
            if (!lastDelim.equals(";") && !lastDelim.equals(","))
                System.out.println();
        } else handleErr(SYNTAX);
    }

    // GOTO 구문 실행
    private void execGoto() throws InterpreterException {
        Integer loc;

        getToken(); // 이동할 레이블을 얻음

        // 레이블의 위치 검색
        loc = (Integer) labelTable.get(token);

        if (loc == null)
            handleErr(UNDEFLABEL); // 정의되지 않은 레이블
        else // loc에서 프로그램 실행 시작
            progIdx = loc.intValue();
    }

    // IF 구문 실행
    private void execIf() throws InterpreterException {
        double result;

        result = evaluate(); // 수식의 값 얻음

        /* 결과가 참인 경우(0이 아닐 때), IF의 목표(target)를 실행하고
           그렇지 않으면 프로그램의 다음 줄로 이동 */
        if (result != 0.0) {
            getToken();
            if (kwToken != THEN) {
                handleErr(THENEXPECTED);
                return;
            }
        } else findEOL(); // 다음 줄의 시작 지점을 검색
    }

    // FOR 루프 실행
    private void execFor() throws InterpreterException {
        ForInfo stckvar = new ForInfo();
        double value;
        char vname;

        getToken(); // 제어 변수를 읽음
        vname = token.charAt(0);
        if (!Character.isLetter(vname)) {
            handleErr(NOTVAR);
            return;
        }

        // 제어 변수의 인덱스를 저장
        stckvar.var = Character.toUpperCase(vname) - 'A';

        getToken(); // 등호를 읽음
        if (token.charAt(0) != '=') {
            handleErr(EQUALEXPECTED);
            return;
        }

        value = evaluate(); // 초깃값 얻음

        vars[stckvar.var] = value;

        getToken(); // TO를 읽은 다음 버림
        if (kwToken != TO) handleErr(TOEXPECTED);

        stckvar.target = evaluate(); // 목표(target) 값을 얻음

        /* 루프가 최소한 한 번 실행될 수 있으면
           스택에 정보를 저장 */
        if (value >= vars[stckvar.var]) {
            stckvar.loc = progIdx;
            fStack.push(stckvar);
        }
        else // 그렇지 않으면 루프 코드를 빠져나감
            while (kwToken != NEXT) getToken();
    }

    // NEXT 구문 실행
    private void next() throws InterpreterException {
        ForInfo stckvar;

        try {
            // 현재의 For 루프를 위한 정보를 검색
            stckvar = (ForInfo) fStack.pop();
            vars[stckvar.var]++; // 제어 변수를 증가시킴

            // 만약 끝났다면 리턴
            if (vars[stckvar.var] > stckvar.target) return;

            // 그렇지 않으면 정보를 회복시킴
            fStack.push(stckvar);
            progIdx = stckvar.loc; // 루프
        } catch (EmptyStackException e) {
            handleErr(NEXTWITHOUTFOR);
        }
    }

    // 간단한 형태의 INPUT을 실행
    private void input() throws InterpreterException {
        int var;
        double val = 0.0;
        String str;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        getToken(); // 프롬포트 문자열이 존재하는지 확인
        if (tokType == QUOTEDSTR) {
            // 만약 있다면 프롬포트를 출력한 다음 쉼표를 검사
            System.out.print(token);
            getToken();
            if (!token.equals(",")) handleErr(SYNTAX);
            getToken();
        }
        else System.out.print("? "); // 그렇지 않다면 '?'를 프롬포트로 출력

        // var 값을 얻음
        var = Character.toUpperCase(token.charAt(0)) - 'A';

        try {
            str = br.readLine();
            val = Double.parseDouble(str); // 값을 읽음
        } catch (IOException exc) {
            handleErr(INPUTIOERROR);
        } catch (NumberFormatException exc) {
            // 이 에러를 다른 에러와 다르게 처리할 수도 있음
            System.out.println("Invalid input.");
        }

        vars[var] = val; // 값을 저장
    }

    // GOSUB를 실행
    private void gosub() throws InterpreterException {
        Integer loc;

        getToken();

        // 호출할 레이블을 찾음
        loc = (Integer) labelTable.get(token);

        if (loc == null)
            handleErr(UNDEFLABEL); // 정의되지 않은 레이블
        else {
            // 리털될 위치를 저장
            gStack.push(Integer.valueOf(progIdx));

            // loc에 저장된 위치에서 프로그램 실행을 시작
            progIdx = loc.intValue();
        }
    }

    // GOSUB에서 리턴
    private void greturn() throws InterpreterException {
        Integer t;

        try {
            // 프로그램 인덱스를 복구
            t = (Integer) gStack.pop();
            progIdx = t.intValue();
        } catch (EmptyStackException exc) {
            handleErr(RETURNWITHOUTGOSUB);
        }
    }

    // REPEAT 구문 처리
    private void repeat() throws InterpreterException {
        // 현재 위치를 스택에 저장
        RUInfo ruInfo = new RUInfo();
        ruInfo.loc = progIdx; // 루프 시작 위치 저장
        ruStack.push(ruInfo);

        getToken(); // 다음 토큰으로 이동
    }

    // UNTIL 구문 처리
    private void until() throws InterpreterException {
        // 스택에서 가장 최근의 REPEAT 정보를 가져옴
        RUInfo ruInfo = (RUInfo) ruStack.pop();

        // UNTIL 조건 계산
        double condition = evaluate();

        if (condition == 0.0) {
            // 조건이 거짓이면 루프 시작 위치로 이동
            progIdx = ruInfo.loc;
            ruStack.push(ruInfo); // 정보를 다시 스택에 저장
        }

        getToken(); // 다음 토큰으로 이동
    }

    // **************** 수식 파서 ****************

    // 파서의 진입점
    private double evaluate() throws InterpreterException {
        double result = 0.0;

        getToken();
        if (token.equals(EOP))
            handleErr(NOEXP); // 수식이 존재하지 않음

        // 수식을 분석해서 계산
        result = evalExp1();

        putBack();

        return result;
    }

    // 관계 연산자를 처리
    private double evalExp1() throws InterpreterException {
        double l_temp, r_temp, result;
        char op;

        result = evalExp2();
        // 프로그램의 끝이면, 리턴
        if (token.equals(EOP)) return result;
        op = token.charAt(0);

        if (isRelop(op)) {
            l_temp = result;
            getToken();
            r_temp = evalExp1();
            switch (op) { // 관계 연산자를 수행
                case '<':
                    if (l_temp < r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case LE:
                    if (l_temp <= r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case '>':
                    if (l_temp > r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case GE:
                    if (l_temp >= r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case '=':
                    if (l_temp == r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case NE:
                    if (l_temp != r_temp) result = 1.0;
                    else result = 0.0;
                    break;
            }
        }
        return result;
    }

    // 두 항목을 더하거나 뺌
    private double evalExp2() throws InterpreterException {
        char op;
        double result;
        double partialResult;

        result = evalExp3();

        while ((op = token.charAt(0)) == '+' || op == '-') {
            getToken();
            partialResult = evalExp3();
            switch (op) {
                case '-':
                    result = result - partialResult;
                    break;
                case '+':
                    result = result + partialResult;
                    break;
            }
        }
        return result;
    }

    // 두 인자를 곱하거나 나눔
    private double evalExp3() throws InterpreterException {
        char op;
        double result;
        double partialResult;

        result = evalExp4();

        while ((op = token.charAt(0)) == '*' || op == '/' || op == '%') {
            getToken();
            partialResult = evalExp4();
            switch (op) {
                case '*':
                    result = result * partialResult;
                    break;
                case '/':
                    if (partialResult == 0.0)
                        handleErr(DIVBYZERO);
                    result = result / partialResult;
                    break;
                case '%':
                    if (partialResult == 0.0)
                        handleErr(DIVBYZERO);
                    result = result % partialResult;
                    break;
            }
        }
        return result;
    }

    // 지수를 처리함
    private double evalExp4() throws InterpreterException {
        double result;
        double partialResult;
        double ex;
        int t;

        result = evalExp5();

        if (token.equals("^")) {
            getToken();
            partialResult = evalExp4();
            ex = result;
            if (partialResult == 0.0) {
                result = 1.0;
            } else
                for (t = (int) partialResult; t > 0; t--)
                    result = result * ex;
        }
        return result;
    }

    // 단항 연산자 + 또는 -를 계산
    private double evalExp5() throws InterpreterException {
        double result;
        String op;

        op = "";
        if ((tokType == DELIMITER) && token.equals("+") || token.equals("-")) {
            op = token;
            getToken();
        }
        result = evalExp6();

        if (op.equals("-")) result = -result;

        return result;
    }

    // 괄호가 있는 수식 처리
    private double evalExp6() throws InterpreterException {
        double result;

        if (token.equals("(")) {
            getToken();
            result = evalExp2();
            if(!token.equals(")"))
                handleErr(UNBALPARENS);
            getToken();
        }
        else result = atom();

        return result;
    }

    // 숫자나 변수의 값을 얻어옴
    private double atom() throws InterpreterException {
        double result = 0.0;

        switch (tokType) {
            case NUMBER:
                try {
                    result = Double.parseDouble(token);
                } catch (NumberFormatException exc) {
                    handleErr(SYNTAX);
                }
                getToken();
                break;
            case VARIABLE:
                result = findVar(token);
                getToken();
                break;
            default:
                handleErr(SYNTAX);
                break;
        }
        return result;
    }

    // 변수의 값을 리턴
    private double findVar(String vname) throws InterpreterException {
        if (!Character.isLetter(vname.charAt(0))) {
            handleErr(SYNTAX);
            return 0.0;
        }
        return vars[Character.toUpperCase(vname.charAt(0)) - 'A'];
    }

    // 입력 스트림으로 토큰을 리턴
    private void putBack() {
        if (token == EOP) return;
        for (int i = 0; i < token.length(); i++) progIdx--;
    }

    // 에러 처리
    private void handleErr(int error) throws InterpreterException {
        String[] err = {
                "Syntax Error",
                "Unbalanced Parentheses",
                "No Expression Present",
                "Division by Zero",
                "Equal sign expected",
                "Not a variable",
                "Label table full",
                "Duplicate label",
                "Undefined label",
                "THEN expected",
                "TO expected",
                "NEXT without FOR",
                "RETURN without GOSUB",
                "Closing quotes needed",
                "File not found",
                "I/O error while loading file",
                "I/O error on INPUT statement"
        };

        throw new InterpreterException(err[error]);
    }

    // 다음 토큰을 읽음
    private void getToken() throws InterpreterException {
        char ch;

        tokType = NONE;
        token = "";
        kwToken = UNKNCOM;

        // 프로그램의 끝인지 검사
        if (progIdx == prog.length) {
            token = EOP;
            return;
        }

        // 공백 문자는 건너뜀
        while (progIdx < prog.length && isSpaceOrTab(prog[progIdx])) progIdx++;

        // 프로그램 뒤에 공백 문자가 붙어 있으면 종료
        if (progIdx == prog.length) {
            token = EOP;
            tokType = DELIMITER;
            return;
        }

        if (prog[progIdx] == '\r') { // crlf를 처리
            progIdx += 2;
            kwToken = EOL;
            token = "\r\n";
            return;
        }

        // 관계 연산자에 대한 검사
        ch = prog[progIdx];
        if (ch == '<' || ch == '>') {
            if (progIdx + 1 == prog.length) handleErr(SYNTAX);
            switch (ch) {
                case '<':
                    if (prog[progIdx + 1] == '>') {
                        progIdx += 2;
                        token = String.valueOf(NE);
                    }
                    else if (prog[progIdx + 1] == '=') {
                        progIdx += 2;
                        token = String.valueOf(LE);
                    }
                    else {
                        progIdx++;
                        token = "<";
                    }
                    break;
                case '>':
                    if (prog[progIdx + 1] == '=') {
                        progIdx += 2;
                        token = String.valueOf(GE);
                    }
                    else {
                        progIdx++;
                        token = ">";
                    }
                    break;
            }
            tokType = DELIMITER;
            return;
        }

        if (isDelim(prog[progIdx])) {
            // 연산자인 경우
            token += prog[progIdx];
            progIdx++;
            tokType = DELIMITER;
        }
        else if (Character.isLetter(prog[progIdx])) {
            // 변수 또는 키워드인 경우
            while (!isDelim(prog[progIdx])) {
                token += prog[progIdx];
                progIdx++;
                if (progIdx >= prog.length) break;
            }
            kwToken = lookUp(token);
            if (kwToken == UNKNCOM) tokType = VARIABLE;
            else tokType = COMMAND;
        }
        else if (Character.isDigit(prog[progIdx])) {
            // 숫자인 경우
            while (!isDelim(prog[progIdx])) {
                token += prog[progIdx];
                progIdx++;
                if (progIdx >= prog.length) break;
            }
            tokType = NUMBER;
        }
        else if (prog[progIdx] == '"') {
            // 인용부호가 있는 문자열인 경우
            progIdx++;
            ch = prog[progIdx];
            while(ch != '"' && ch != '\r') {
                token += ch;
                progIdx++;
                ch = prog[progIdx];
            }
            if (ch == '\r') handleErr(MISSINGQUOTE);
            progIdx++;
            tokType = QUOTEDSTR;
        }
        else { // 정의되지 않은 문자인 경우 프로그램 종료
            token = EOP;
            return;
        }
    }

    // c가 구분자(delimiter)이면 true를 리턴
    private boolean isDelim(char c) {
        if ((" \r,;<>+-/*%^=()".indexOf(c) != -1))
            return true;
        return false;
    }

    // c가 공백(space)이거나 탭(tab)이면 true를 리턴
    boolean isSpaceOrTab(char c) {
        if (c == ' ' || c == '\t') return true;
        return false;
    }

    // c가 관계 연산자이면 true를 리턴
    boolean isRelop(char c) {
        if (relops.indexOf(c) != -1) return true;
        return false;
    }

    // 토큰 테이블에서 토큰의 내부 표현을 검색
    private int lookUp(String s) {
        int i;

        // 소문자로 변환
        s = s.toLowerCase();

        // 테이블에 토큰이 있는지 검사
        for (i = 0; i < kwTable.length; i++)
            if (kwTable[i].keyword.equals(s))
                return kwTable[i].keywordTok;
        return UNKNCOM; // 알려지지 않은 키워드
    }
}
