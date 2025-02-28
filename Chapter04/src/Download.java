import java.io.*;
import java.net.*;
import java.util.*;

// 이 클래스는 URL에서부터 파일을 다운로드한다.
class Download extends Observable implements Runnable {
    // 다운로드 버퍼의 최댓값
    private static final int MAX_BUFFER_SIZE = 1024;

    // 상태 코드 이름
    public static final String STATUSES[] = { "Downloading", "Paused", "Complete", "Cancelled", "Error" };

    // 상태 코드 이름
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private URL url;        // 다운로드할 URL
    private int size;       // 다운로드할 파일의 크기(바이트 단위)
    private int downloaded; // 다운로드한 크기(바이트 단위)
    private int status;     // 다운로드의 현재 상태

    // Download의 생성자
    public Download(URL url) {
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;

        // 다운로드를 시작
        download();
    }

    // 다운로드할 URL을 얻음
    public String getUrl() {
        return url.toString();
    }

    // 다운로드할 파일의 크기를 얻음
    public int getSize() {
        return size;
    }

    // 다운로드의 진행률을 얻음
    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    // 다운로드의 상태를 얻음
    public int getStatus() {
        return status;
    }

    // 다운로드를 정지
    public void pause() {
        status = PAUSED;
        stateChanged();
    }

    // 다운로드를 재개
    public void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }

    // 다운로드를 취소
    public void cancel() {
        status = CANCELLED;
        stateChanged();
    }

    // 이 다운로드에 오류가 있음을 표시
    private void error() {
        status = ERROR;
        stateChanged();
    }

    // 다운로드를 시작하거란 재개
    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    // URL에서 파일 이름 부분을 얻음
    private String getFileName(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    // 파일을 다운로드함
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;

        try {
            // URL에 대한 연결을 여는 작업
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 파일의 어느 부분을 다운로드할 것인지 명세
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");

        // 서버에 접속
        connection.connect();

        // 응답 코드가 200번 대에 있는지 확인
        if (connection.getResponseCode() / 100 != 2) {
            error();
        }

        // 유효한 content length를 검사
        int contentLength = connection.getContentLength();
        if (contentLength < 1) {
            error();
        }

        // 아직 다운로드에 대한 크기가 설정되지 않았으면 설정
        if (size == -1) {
            size = contentLength;
            stateChanged();
        }

        // 파일을 연 다음, 파일 포인터를 파일의 끝으로 이동
        file = new RandomAccessFile(getFileName(url), "rw");
        file.seek(downloaded);

        stream = connection.getInputStream();
        while (status == DOWNLOADING) {
            /* 다운로드할 부분이 얼마나 더 남아 있는지에 따라
               버퍼의 크기를 조절 */
            byte buffer[];
            if (size - downloaded > MAX_BUFFER_SIZE) {
                buffer = new byte[MAX_BUFFER_SIZE];
            } else {
                buffer = new byte[size - downloaded];
            }

            // 서버에서부터 버퍼로 읽어옴
            int read = stream.read(buffer);
            if (read == -1)
                break;

            // 버퍼의 내용을 파일에 씀
            file.write(buffer, 0, read);
            downloaded += read;
            stateChanged();
        }

        /* 이 지점에 도달하면 다운로드가 끝났음을 의미하므로
           상태값을 완료(complete)로 바꿈 */
        if (status == DOWNLOADING) {
            status = COMPLETE;
            stateChanged();
        }
        } catch (Exception e) {
            error();
        } finally {
            // 파일을 닫음
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {}
            }

            // 서버에 대한 연결을 닫음
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
        }
    }

    // 이 다운로드의 상태가 변경됐음을 관찰자(observer)들에게 알림
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}
