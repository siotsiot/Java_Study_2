# 프로그램 컴파일 및 실행 방법
> 컴파일과 실행은 반드시 소스 코드가 포함되어 있는 디렉토리에서 작업해야 합니다.

## 컴파일
IDE에 기본적으로 터미널이 포함되어 있으면 터미널 실행 시 바로 현재 작업 중인 디렉토리로 설정되므로, 소스 코드 파일이 있는 위치로 이동하지 않아도 됩니다.
> Windows 명령 프롬포트에서 실행하려면 다음과 같이 수행해야 합니다.
> 1. 소스 코드 파일이 있는 디렉토리로 이동합니다.
> 2. 터미널에서 다음과 같이 입력합니다.
```
javac DownloadManager.java DownloadsTableModel.java ProgressRenderer.java Download.java
```
> 만약 한글 주석이 포함되어 있으면 다음과 같이 입력합니다.
```
javac -encoding UTF-8 DownloadManager.java DownloadsTableModel.java ProgressRenderer.java Download.java
```

## 실행 방법
터미널에서 다음과 같이 입력합니다.
```
javaw DownloadManager
```

## 파일 다운로드 참고 사이트
다음 사이트에서 프로그램 테스트를 진행했습니다.
> [사이트](https://sample-videos.com/index.php#sample-mp4-video)
