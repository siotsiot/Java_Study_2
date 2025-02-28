import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

// 다운로드 매니저
public class DownloadManager extends JFrame implements Observer {
    // 다운로드를 추가하는 텍스트 필드
    private JTextField addTextField;

    // 다운로드 테이블의 데이터 모델
    private DownloadsTableModel tableModel;

    // 다운로드 리스트 테이블
    private JTable table;

    // 선택된 다운로드를 관리하는 버튼들
    private JButton pauseButton, resumeButton;
    private JButton cancelButton, clearButton;

    // 현재 선택된 다운로드
    private Download selectedDownload;

    // 테이블 선택이 삭제되었는지 여부를 나타내는 플래그
    private boolean clearing;

    // 생성자
    public DownloadManager() {
        // 애플리케이션의 제목을 설정
        setTitle("Download Manager");

        // 윈도우의 크기를 설정
        setSize(640, 480);

        // 윈도우가 닫힐 때의 이벤트를 처리
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });

        // 파일 메뉴를 생성
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // add 패널을 설정
        JPanel addPanel = new JPanel();
        addTextField = new JTextField(30);
        addPanel.add(addTextField);
        JButton addButton = new JButton("Add Download");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionAdd();
            }
        });
        addPanel.add(addButton);

        // Downloads 테이블을 설정
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                tableSelectionChanged();
            }
        });
        // 한 번에 한 행만 선택되도록 함
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // ProgressBar를 Progress 열의 렌더러(Renderer)로 설정
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true); // progress 텍스트를 보여줌
        table.setDefaultRenderer(JProgressBar.class, renderer);

        // JProgressBar에 맞도록 테이블 행의 높이를 충분히 크게 설정
        table.setRowHeight((int) renderer.getPreferredSize().getHeight());

        // downloads 패널을 설정
        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(BorderFactory.createTitledBorder("Downloads"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // buttons 패널을 설정
        JPanel buttonsPanel = new JPanel();
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionPause();
            }
        });
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);
        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionResume();
            }
        });
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionClear();
            }
        });
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionClear();
            }
        });
        clearButton.setEnabled(false);
        buttonsPanel.add(clearButton);

        // 출력할 패널들을 추가
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    // 프로그램을 종료
    private void actionExit() {
        System.exit(0);
    }

    // 새로운 다운로드를 추가
    private void actionAdd() {
        URL verifiedUrl = verifyUrl(addTextField.getText());
        if (verifiedUrl != null) {
            tableModel.addDownload(new Download(verifiedUrl));
            addTextField.setText(""); // add 텍스트 필드를 리셋함
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Download URL", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 다운로드할 URL을 확인
    private URL verifyUrl(String url) {
        // HTTP URL만을 허용함
        /*
         * 현재 거의 대부분의 웹사이트가 https://를 지원하므로 http를 https로 변경
         */
        // if (!url.toLowerCase().startsWith("http://"))
        if (!url.toLowerCase().startsWith("https://"))
            return null;
        
        // URL의 형식을 검증
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        // URL이 파일을 지정하고 있는지 확인
        if (verifiedUrl.getFile().length() < 2)
            return null;
        
        return verifiedUrl;
    }

    // 테이블의 행 선택이 바뀔 때 호출됨
    private void tableSelectionChanged() {
        /* 마지막으로 선택된 다운로드로부터 
           통보(notification)받기를 취소함 */
        if (selectedDownload != null)
            selectedDownload.deleteObserver(DownloadManager.this);
        
        /* 다운로드를 리스트에서 제거하는 중이 아니라면
           선택된 다운로드를 설정한 다음, 그 다운로드가 
           현재 객체로부터 통보를 받을 수 있도록 설정 */
        if (!clearing) {
            selectedDownload = tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
        }
    }

    // 선택된 다운로드를 정지
    private void actionPause() {
        selectedDownload.pause();
        updateButtons();
    }

    // 선택된 다운로드를 재개
    private void actionResume() {
        selectedDownload.resume();
        updateButtons();
    }

    // 선택된 다운로드를 취소
    private void actionCancel() {
        selectedDownload.cancel();
        updateButtons();
    }

    // 선택된 다운로드를 리스트에서 삭제
    private void actionClear() {
        clearing = true;
        tableModel.clearDownload(table.getSelectedRow());
        clearing = false;
        selectedDownload = null;
        updateButtons();
    }

    /* 현재 선택된 다운로드의 상태에 기반해서서
       각 버튼의 상태를 갱신함 */
    private void updateButtons() {
        if (selectedDownload != null) {
            int status = selectedDownload.getStatus();
            switch (status) {
                case Download.DOWNLOADING:
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.PAUSED:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.ERROR:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    break;
                default: // COMPLETE 또는 CANCELLED
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    break;
            }
        } else {
            // 테이블에 있는 어떤 다운로드도 선택되지 않은 경우
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            clearButton.setEnabled(false);
        }
    }

    /* Download에 대한 변화가 일어나서 Download 객체가
       관찰자(observer)들에게 통보할 때 호출됨 */
    public void update(Observable o, Object arg) {
        // Update buttons if the selected download has changed.
        if (selectedDownload != null && selectedDownload.equals((o)))
            updateButtons();
    }

    // 다운로드 매니저를 실행시킴
    public static void main(String[] args) {
        DownloadManager manager = new DownloadManager();
        manager.show();
    }
}
