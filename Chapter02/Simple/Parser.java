/*
    이 모듈은 변수형을 사용하지 않는 재귀적 용법의 파서를 포함한다.
 */

// 파서 에러 처리를 위한 Exception 클래스
class ParserException extends Exception {
    String errStr; // 에러 정의 문자열

    /**
     * 생성자
     */
    public ParserException(String str) {
        errStr = str;
    }

    /**
     * toString() 메소드를 오버라이딩하여 문자열 객체를 문자열로 출력
     */
    public String toString() {
        return errStr;
    }
}

class Parser {
    // TOKEN 타입(종류) 상수값
    final int NONE = 0;
    final int DELIMITER = 1;
    final int VARIABLE = 2;
    final int NUMBER = 3;

    // 에러 종류에 대한 상수값
    final int SYNTAX = 0;
    final int UNBALPARENS = 1;
    final int NOEXP = 2;
    final int DIVBYZERO = 3;

    // 표현식의 끝을 나타내는 상수
    final String EOE = "\0";

    private String exp;   // 표현(expression)을 담고 있는 문자열
    private int expIdx;   // 표현(expression)의 현재 인덱스
    private String token; // 현재 인덱싱 된 토큰
    private int tokType;  // 현재 인덱싱 된 토큰의 타입

    // 파서의 시작점
    public double evaluate(String expstr) throws ParserException {
        double result;
        exp = expstr;
        expIdx = 0;

        /**
         * getToken();
         * 토큰값을 가져옴
         * 
         * if (token.equals(EOE))
         * - equals() 메소드는 두 객체가 같은 값을 가지고 있는지 비교
         * - getToken()으로 가져온 토큰값(token)이 표현식의 끝(EOE; End Of Expression)이면
         * 
         * handleErr(NOEXP);
         * NOEXP의 값을 2로 정의(상수)했으므로 
         * handleErr() 메소드의 err 문자열 배열의 인덱스 2에 해당하는 "No Expression Present" 출력
         */
        getToken();
        if (token.equals(EOE))
            handleErr(NOEXP); // 표현이 존재하지 않음

        /**
         * evalExp2() 메소드(더하거나 빼는 계산)를 호출해 계산 수행
         */
        // 표현을 파싱하고 값을 구한다.
        result = evalExp2();

        if (!token.equals(EOE)) // 문자열의 마지막은 EOE이어야 한다.
            handleErr(SYNTAX);

        return result;
    }

    // 더하거나 뺀다.
    private double evalExp2() throws ParserException {
        /**
         * char op;
         * 연산자
         * double result;
         * 
         */
        char op;
        double result;
        double partialResult;

        /**
         * 연산자의 우선순위를 고려하여 evalExp3() 메소드(곱셈, 나눗셈 등 계산 등) 호출
         */
        result = evalExp3();

        /**
         * while ((op = token.charAt(0)) == '+' || op == '-')
         * token.charAt(0)은 토큰의 첫 번째 문자가 '+' 또는 '-'이면 반복
         * 
         * getToken();
         * 연산자 다음의 숫자를 읽기 위해 다음 토큰 가져옴
         * 
         * partialResult = evalExp3();
         * 연산자의 오른쪽 피연산자 값을 구하기 위해 evalExp3() 호출
         * 
         * switch 문을 사용하여 덧셈 또는 뺄셈 연산 수행
         * 
         * return result;
         * 계산된 값 반환
         */
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

    // 곱하거나 나눈다.
    private double evalExp3() throws ParserException {
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

    // 지수를 처리한다.
    private double evalExp4() throws ParserException {
        double result;
        double partialResult;
        double ex;
        int t;

        result = evalExp5();

        if (token.equals("^")) {
            getToken();
            partialResult = evalExp4();
            ex = result;
            if (partialResult == 0.0)
                result = 1.0;
            else
                for (t = (int) partialResult - 1; t > 0; t--)
                    result = result * ex;
        }
        return result;
    }

    // 단항의 +, -를 처리한다.
    private double evalExp5() throws ParserException {
        double result;
        String op;

        op = "";
        if ((tokType == DELIMITER) && token.equals("+") || token.equals("-")) {
            op = token;
            getToken();
        }
        result = evalExp6();

        if (op.equals("-"))
            result = -result;

        return result;
    }

    // 괄호를 처리한다.
    private double evalExp6() throws ParserException {
        double result;

        if (token.equals("(")) {
            getToken();
            result = evalExp2();
            if (!token.equals(")"))
                handleErr(UNBALPARENS);
            getToken();
        } else
            result = atom();

        return result;
    }

    // 숫자값을 구한다.
    private double atom() throws ParserException {
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
            default:
                handleErr(SYNTAX);
                break;
        }
        return result;
    }

    // 에러를 처리한다.
    private void handleErr(int error) throws ParserException {
        String[] err = { "Syntax Error", "Unbalanced Parentheses", "No Expression Present", "Division by Zero" };

        throw new ParserException(err[error]);
    }

    // 토큰값을 가져온다.
    private void getToken() {
        /**
         * 토큰값을 초기화한다.
         */
        tokType = NONE;
        token = "";

        /**
         * expIdx == exp.length()
         * 현재 표현식의 인덱스(expIdx)가 표현식의 범위위(exp.length())와 같으면 
         * 
         * token = EOE;
         * return;
         * 토큰을 파일의 끝(EOE; End Of Expression)으로 설정 후, getToken() 메소드 종료
         * 
         */
        // 표현의 끝인지 여부를 확인한다.
        if(expIdx == exp.length()) {
            token = EOE;
            return;
        }

        /**
         * expIdx < exp.length()
         * 현재 표현식의 인덱스(expIdx)가 표현식의 범위위(exp.length()) 안에 있고,
         * Character.isWhiteSpace(exp.charAt(expIdx))
         * - Caracter 클래스의 isWhiteSpace() 메소드는 ' '(스페이스), '\t'(탭), \n'(줄바꿈), '\r'(캐리지 리턴), 기타 공백 문자(유니코드 공백 포함)을 공백으로 인식
         * - exp.charAt(expIdx)는 현재 표현식의 인덱스(expIdx)번째의 문자
         * 
         * ++expIdx;
         * 다음 표현식의 인덱스로 이동동
         */
        // 공백을 넘어간다.
        while(expIdx < exp.length() && Character.isWhitespace(exp.charAt(expIdx)))
            ++expIdx;

        // 표현의 마지막인지 확인한다.
        if(expIdx == exp.length()) {
            token = EOE;
            return;
        }

        /**
         * isDelim(exp.charAt(expIdx))
         * 현재 표현식의 인덱스의 문자가 연산자면
         * 
         * token += exp.charAt(expIdx);
         * 토큰에 현재 표현식 인덱스의 문자 추가
         * 
         * expIdx++;
         * 다음 문자로 이동
         * 
         * tokType = DELIMITER;
         * 현재 토큰은 연산자 타입으로 설정
         */
        if(isDelim(exp.charAt(expIdx))) { // 연산자형
            token += exp.charAt(expIdx);
            expIdx++;
            tokType = DELIMITER;
        }
        /**
         * Character.isLetter(exp.charAt(expIdx))
         * - Character 클래스의 isLetter() 메소드는 해당 문자가 문자(알파벳, 한글 등)인지 확인하는 역할
         * - 문자면 true, 문자가 아니면 false 반환환
         * - 현재 표현식 인덱스의 문자가 문자(알파벳, 한글 등)이면
         * 
         * while(!isDelim(exp.charAt(expIdx)))
         * - 현재 표현식 인덱스의 문자가 구분자가 아닐 때까지 반복
         * - 구분자가 등장하면 while 구문 종료 -> 변수명이 문자열이어도 처리됨
         * 
         * token += exp.charAt(expIdx);
         * - 토큰에 현재 표현식 인덱스의 문자 추가
         * 
         * expIdx++;
         * 다음 문자로 이동
         * 
         * if(expIdx >= exp.length())
         * 현재 표현식 인덱스가 표현식 범위를 벗어나면 종료
         * 
         * tokType = VARIABLE;
         * 토큰 타입을 변수(VARIABLE)로 설정
         */
        else if(Character.isLetter(exp.charAt(expIdx))) { // 변수형
            while(!isDelim(exp.charAt(expIdx))) {
                token += exp.charAt(expIdx);
                expIdx++;
                if(expIdx >= exp.length())
                    break;
            }
            tokType = VARIABLE;
        }
        /**
         * Character.isDigit(exp.charAt(expIdx))
         * - Character 클래스의 isDigit() 메소드는 해당 문자가 숫자(0-9)인지 확인하는 역할
         * - 숫자면 true, 숫자가 아니면 false 반환
         * - 현재 표현식 인덱스의 문자가 숫자이면
         * 
         * while(!isDelim(exp.charAt(expIdx)))
         * 위 분기문 중 변수형과 과정 동일
         * 
         * tokType = NUMBER;
         * 토큰 타입을 숫자(NUMBER)로 설정
         */
        else if(Character.isDigit(exp.charAt(expIdx))) { // 숫자형
            while(!isDelim(exp.charAt(expIdx))) {
                token += exp.charAt(expIdx);
                expIdx++;
                if (expIdx >= exp.length())
                    break;
            }
            tokType = NUMBER;
        }
        /**
         * 연산자형, 변수형, 숫자형이 아닌 표현식이면
         * 
         * token = EOE;
         * 토큰을 표현식의 끝(EOE; End Of Expression)으로 간주하고 getToken() 메소드 종료
         */
        else // 기타 정의되지 않은 형이라면 표현식이 종료된 것으로 간주한다.
        {
            token = EOE;
            return;
        }
    }

    /**
     * 주어진 문자 c가 연산자(구분 문자(Delimiter))인지 확인하는 역할
     * 
     * " +-/*^=()".indexOf(c) != -1
     * - indexOf() 메소드는 문자열에서 인수에 해당하는 위치를 반환
     * - 해당 연산자가 있으면 true, 없으면 false를 반환
     */
    // true가 리턴되면 c는 연산자(일반 연산자, 괄호, 스페이스) 문자이다.
    private boolean isDelim(char c) {
        if((" +-/*^=()".indexOf(c) != -1))
            return true;
        return false;
    }
}
