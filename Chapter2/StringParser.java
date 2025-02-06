/*
파서 소스
 */

// 에러 처리를 위한 Exception 클래스
class ParserException extends Exception {
    String errStr; // 에러 기술

    public ParserException(String str) {
        errStr = str;
    }

    public String toString() {
        return errStr;
    }
}

class StringParser {
    // 토큰 타입 상수값
    final int NONE = 0;
    final int DELIMITER = 1;
    final int VARIABLE = 2;
    final int NUMBER = 3;
    final int STRING = 4;

    // 에러 타입 상수
    final int SYNTAX = 0;
    final int UNBALPARENS = 1;
    final int NOEXP = 2;
    final int DIVBYZERO = 3;

    // 문자열의 끝을 나타내는 상수
    final String EOE = "\0";

    private String exp;   // 표현식 문자열
    private int expIdx;   // 현재 인덱스 값
    private String token; // 현재 토큰값
    private int tokType;  // 현재 토큰값의 타입

    // 변수를 위한 배열
    private String vars[] = new String[26];

    public String evaluate(String expstr) throws ParserException {
        String result;
        exp = expstr;
        expIdx = 0;

        getToken();
        if (token.equals(EOE))
            handleErr(NOEXP); // 표현식이 존재하지 않음

        // 표현을 파싱하고 값을 구한다.
        result = evalExp1();

        if (!token.equals(EOE)) // 문자열의 마지막은 EOE이어야 한다.
            handleErr(SYNTAX);

        return result;
    }

    private String evalExp1() throws ParserException 
    {  
        String result; 
        int varIdx;  
        int ttokType;  
        String temptoken;  
    
        if(tokType == VARIABLE) {  
            temptoken = new String(token);  
            ttokType = tokType;  
        
            varIdx = Character.toUpperCase(token.charAt(0)) - 'A';  
        
            getToken();  
            if(!token.equals("=")) {  
                putBack();
                token = new String(temptoken);  
                tokType = ttokType;  
            } else {  
                getToken();
                result = evalExp2();  
                vars[varIdx] = result;  
                return result;  
            }  
        }  
        
        return evalExp2();  
    }

    // 더하거나 뺀다.
    private String evalExp2() throws ParserException {
        char op;
        String result;
        String partialResult;

        result = evalExp3();

        while ((op = token.charAt(0)) == '+' || op == '-') {
            getToken();
            partialResult = evalExp3();

            // 문자열 덧셈 처리
            switch (op) {
                case '-':
                    try {
                        result = String.valueOf(Double.parseDouble(result) - Double.parseDouble(partialResult));
                    } catch (NumberFormatException e) {
                        handleErr(SYNTAX);
                    }
                    break;
                case '+':
                    try {
                        result = String.valueOf(Double.parseDouble(result) + Double.parseDouble(partialResult));
                    } catch (NumberFormatException e) {
                        result = result + partialResult;
                    }
                    break;
            }
        }
        return result;
    }

    // 곱하거나 나눈다.
    private String evalExp3() throws ParserException {
        char op;
        String result;
        String partialResult;

        result = evalExp4();

        while ((op = token.charAt(0)) == '*' || op == '/' || op == '%') {
            getToken();
            partialResult = evalExp4();
            if (isNumber(result) && isNumber(partialResult)) {
                switch (op) {
                    case '*':
                        result = String.valueOf(Double.parseDouble(result) * Double.parseDouble(partialResult));
                        break;
                    case '/':
                        if (Double.parseDouble(partialResult) == 0.0)
                            handleErr(DIVBYZERO);
                        result = String.valueOf(Double.parseDouble(result) / Double.parseDouble(partialResult));
                        break;
                    case '%':
                        if (Double.parseDouble(partialResult) == 0.0)
                            handleErr(DIVBYZERO);
                        result = String.valueOf(Double.parseDouble(result) % Double.parseDouble(partialResult));
                        break;
                }
            }
            else
                handleErr(SYNTAX);
        }
        return result;
    }

    // 지수를 처리한다.
    private String evalExp4() throws ParserException {
        String result;
        String partialResult; // 지수
        int t;

        result = evalExp5();

        if (token.equals("^")) {
            getToken();
            partialResult = evalExp4();

            if (isNumber(result) && isNumber(partialResult)) {
                double ex = Double.parseDouble(result);
                double base = Double.parseDouble(partialResult);

                if (base == 0.0)
                    result = "1.0";
                else {
                    double res = 1.0;
                    for (t = (int) ex - 1; t > 0; t--)
                        res = res * base;
                }
            }
            else
                handleErr(SYNTAX);
        }
        return result;
    }

    // 단항의 +, -를 처리한다.
    private String evalExp5() throws ParserException {
        String result;
        String op;

        op = "";
        if ((tokType == DELIMITER) && token.equals("+") || token.equals("-")) {
            op = token;
            getToken();
        }
        result = evalExp6();
        
        try {
            double tempResult = Double.parseDouble(result);

            if (op.equals("-"))
                tempResult = -tempResult;
        } catch (NumberFormatException e) {
            if (!op.isEmpty())
                handleErr(SYNTAX);
        }

        return result;
    }

    // 괄호를 처리한다.
    private String evalExp6() throws ParserException {
        String result;

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

    // 숫자 또는 변수 값 또는 문자열을 가져온다.
    private String atom() throws ParserException {
        String result = "";

        switch (tokType) {
            case NUMBER:
                try {
                    result = String.valueOf(Double.parseDouble(token));
                } catch (NumberFormatException exc) {
                    handleErr(SYNTAX);
                }
                getToken();
                break;
            case VARIABLE:
                result = findVar(token);
                getToken();
                break;
            case STRING:
                result = token;
                getToken();
                break;
            default:
                handleErr(SYNTAX);
                break;
        }
        return result;
    }

    // 변수의 값을 반환한다.
    private String findVar(String vname) throws ParserException {
        if (!Character.isLetter(vname.charAt(0))) {
            handleErr(SYNTAX);
            return "";
        }
        return vars[Character.toUpperCase(vname.charAt(0)) - 'A'];
    }

    // 입력 스트림의 값만큼 인덱스 값을 되돌린다.
    private void putBack() {
        if (token == EOE)
            return;
        for (int i = 0; i < token.length(); i++)
            expIdx--;
    }

    // 오류를 처리한다.
    private void handleErr(int error) throws ParserException {
        String[] err = { "Syntax Error", "Unbalanced Parentheses", "No Expression Present", "Division by Zero" };

        throw new ParserException(err[error]);
    }

    // 토큰값을 가져온다.
    private void getToken() {
        tokType = NONE;
        token = "";

        // 표현의 끝인지 여부를 확인한다.
        if(expIdx == exp.length()) {
            token = EOE;
            return;
        }

        // 공백을 넘어간다.
        while(expIdx < exp.length() && Character.isWhitespace(exp.charAt(expIdx)))
            ++expIdx;

        // 표현의 마지막인지 확인한다.
        if(expIdx == exp.length()) {
            token = EOE;
            return;
        }

        if(isDelim(exp.charAt(expIdx))) { // 연산자형
            token += exp.charAt(expIdx);
            expIdx++;
            tokType = DELIMITER;
        }
        else if(Character.isLetter(exp.charAt(expIdx))) { // 변수형
            while(!isDelim(exp.charAt(expIdx))) {
                token += exp.charAt(expIdx);
                expIdx++;
                if(expIdx >= exp.length())
                    break;
            }
            tokType = VARIABLE;
        }
        else if(Character.isDigit(exp.charAt(expIdx))) { // 숫자형
            while(!isDelim(exp.charAt(expIdx))) {
                token += exp.charAt(expIdx);
                expIdx++;
                if (expIdx >= exp.length())
                    break;
            }
            tokType = NUMBER;
        }
        else if (exp.charAt(expIdx) == '"') { // 문자열형
            // 여는 따옴표는 넘어감
            expIdx++;

            // 문자열 범위 내에 있고, 닫는 따옴표 등장 전까지 반복
            while (expIdx < exp.length() && exp.charAt(expIdx) != '"') {
                token += exp.charAt(expIdx);    // 문자 추가
                expIdx++;                       // 다음 문자로 이동
            }
            expIdx++;           // 닫는 따옴표 넘어감
            tokType = STRING;   // 토큰 타입은 STRING 
        }
        else // 기타 정의되지 않은 형이라면 표현식이 종료된 것으로 간주한다.
        {
            token = EOE;
            return;
        }
    }

    // true가 리턴되면 c는 구획 문자이다.
    private boolean isDelim(char c) {
        if((" +-/*^=()".indexOf(c) != -1))
            return true;
        return false;
    }

    // 숫자인지 확인하는 메소드
    private boolean isNumber(String str) {
        try {
            Double.parseDouble(str);        // 숫자로 변환이 가능하면
            return true;                    // true
        } catch (NumberFormatException e) { // 숫자가 아니면
            return false;                   // false
        }
    }
}
