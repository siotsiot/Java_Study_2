import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

// 이 클래스는 테이블 셀에 JProgressBar를 렌더링함
class ProgressRenderer extends JProgressBar implements TableCellRenderer {
    // ProgressRenderer에 대한 생성자
    public ProgressRenderer(int min, int max) {
        super(min, max);
    }

    /* 주어진 테이블 셀에 대해 이 JProgressBar 객체를
       렌더러(renderer)로서 리턴함 */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // JProgressBar의 완료 백분율 값을 설정
        setValue((int) ((Float) value).floatValue());
        return this;
    }
}
