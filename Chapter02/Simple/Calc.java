// 계산기 애플릿 소스
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
/*
    <applet code="Calc" width=200 height=150
    </applet>
 */

 public class Calc extends Applet implements ActionListener {
    TextField expText, resText;
    Parser p;

    public void init() {
        Label heading = new  Label("Expression Calculator ", Label.CENTER);

        Label explab = new Label("Expression ", Label.CENTER);
        Label reslab = new Label("Result    ", Label.CENTER);
        expText = new TextField(24);
        resText = new TextField(24);

        resText.setEditable(false); // 화면 출력을 위한 결과 필드

        add(heading);
        add(explab);
        add(expText);
        add(reslab);
        add(resText);

        /* text 필드 ActionListener로 등록하기 */
        expText.addActionListener(this);

        // 파서 생성하기
        p = new Parser();
    }

    // Enter 키를 눌렀을 때 처리
    public void actionPerformed(ActionEvent ae) {
        repaint();
    }

    public void paint(Graphics g) {
        double result = 0.0;
        String expstr = expText.getText();

        try {
            if (expstr.length() != 0)
                result = p.evaluate(expstr);
            
            // 다음을 통해 ENTER 키가 눌려진 이후의 표현식 무시하기
            // expText.setText("");

            resText.setText(Double.toString(result));

            showStatus(""); // 이전 오류 메시지 삭제
        } catch (ParserException exc) {
            showStatus(exc.toString());
            resText.setText("");
        }
    }
}
