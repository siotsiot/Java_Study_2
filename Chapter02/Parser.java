/*
    이 모듈은 변수형을 사용하지 않는 재귀적 용법의 파서를 포함한다.
 */

// 파서 에러 처리를 위한 Exception 클래스
class ParserException extends Exception
{
    String errStr; // 에러 정의 문자열

    public ParserException(String str)
    {
        errStr = str;
    }

    public String toString()
    {
        return errStr;
    }
}

class Parser
{
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
    public double evaluate(String expstr) throws ParserException
    {
        double result;
        exp = expstr;
        expIdx = 0;

        getToken();
        if (token.equals(EOE))
            handleErr(NOEXP); // 표현이 존재하지 않음

        // 표현을 파싱하고 값을 구한다.
        result = evalExp2();

        if (!token.equals(EOE)) // 문자열의 마지막은 EOE이어야 한다.
            handleErr(SYNTAX);

        return result;
    }

    // 더하거나 뺀다.
    private double evalExp2() throws ParserException
    {
        char op;
        double result;
        double partialResult;

        result = evalExp3();

        while ((op = token.charAt(0)) == '+' || op == '-')
        {
            getToken();
            partialResult = evalExp3();
            switch (op)
            {
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
    private double evalExp3() throws ParserException
    {
        char op;
        double result;
        double partialResult;

        result = evalExp4();

        while ((op = token.charAt(0)) == '*' || op == '/' || op == '%')
        {
            getToken();
            partialResult = evalExp4();
            switch (op)
            {
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
    private double evalExp4() throws ParserException
    {
        double result;
        double partialResult;
        double ex;
        int t;

        result = evalExp5();

        if (token.equals("^"))
        {
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
    private double evalExp5() throws ParserException
    {
        double result;
        String op;

        op = "";
        if ((tokType == DELIMITER) && token.equals("+") || token.equals("-"))
        {
            op = token;
            getToken();
        }
        result = evalExp6();

        if (op.equals("-"))
            result = -result;

        return result;
    }

    // 괄호를 처리한다.
    private double evalExp6() throws ParserException
    {
        double result;

        if (token.equals("("))
        {
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
    private double atom() throws ParserException
    {
        double result = 0.0;

        switch (tokType)
        {
            case NUMBER:
                try
                {
                    result = Double.parseDouble(token);
                }
                catch (NumberFormatException exc)
                {
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
    private void handleErr(int error) throws ParserException
    {
        String[] err = {"Syntax Error", "Unbalanced Parentheses", "No Expression Present", "Division by Zero"};

        throw new ParserException(err[error]);
    }

    // 토큰값을 가져온다.
    private void getToken()
    {
        tokType = NONE;
        token = "";

        // 표현의 끝인지 여부를 확인한다.
        if (expIdx == exp.length())
        {
            token = EOE;
            return;
        }

        // 공백을 넘어간다.
        while (expIdx < exp.length() && Character.isWhitespace(exp.charAt(expIdx)))
            ++expIdx;

        // 표현의 마지막인지 확인한다.
        if (expIdx == exp.length())
        {
            token = EOE;
            return;
        }

        if (isDelim(exp.charAt(expIdx))) // 연산자
        {
            token += exp.charAt(expIdx);
            expIdx++;
            tokType = DELIMITER;
        }
        else if (Character.isLetter(exp.charAt(expIdx))) // 변수형
        {
            while (!isDelim(exp.charAt(expIdx)))
            {
                token += exp.charAt(expIdx);
                expIdx++;
                if (expIdx >= exp.length())
                    break;
            }
            tokType = VARIABLE;
        }
        else if (Character.isDigit(exp.charAt(expIdx))) // 숫자형
        {
            while (!isDelim(exp.charAt(expIdx)))
            {
                token += exp.charAt(expIdx);
                expIdx++;
                if (expIdx >= exp.length())
                    break;
            }
            tokType = NUMBER;
        }
        else // 기타 정의되지 않은 형이라면 표현식이 종료된 것으로 간주한다.
        {
            token = EOE;
            return;
        }
    }

    // true가 리턴되면 c는 연산자(일반 연산자, 괄호, 스페이스) 문자이다.
    private boolean isDelim(char c)
    {
        if ((" +-/*^=()".indexOf(c) != -1))
            return true;
        return false;
    }
}
