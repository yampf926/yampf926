import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int PORT = 9260;
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAUNCHER_DIR = ROOT.resolve("launchers");

    private record Project(
            String id,
            String title,
            String type,
            String description,
            String detail,
            String tech,
            String folder,
            String accent,
            boolean webProject,
            String liveUrl
    ) {
    }

    private static final List<Project> PROJECTS = List.of(
            new Project("dohwa", "Dohwa 웹사이트", "웹 프로젝트", "React/Vite와 Spring Boot로 만든 팬 커뮤니티형 웹페이지", "공연 예매, 팬 게시판, 채팅, 알림 흐름을 구성하고 공개 페이지는 정적 데모 API로 실행", "React · Vite · Spring Boot · 정적 데모 API", "src\\Dohwa\\frontend", "#c9b8ff", true, "https://yampf926.github.io/yampf926/dohwa/"),
            new Project("game", "보드게임 컬렉션", "웹 프로젝트", "Java HttpServer에서 출발해 브라우저에서 실행되도록 정리한 보드게임 허브", "회원 정보, 게임 선택, 활동 기록을 localStorage로 보관하며 공개 페이지에서 바로 플레이 가능", "Java HttpServer · HTML · CSS · JavaScript", "src\\Game", "#73d7ff", true, "https://yampf926.github.io/yampf926/boardgame/"),
            new Project("choice", "비주얼 노벨 게임", "Java 게임", "이미지와 선택지를 사용하는 스토리 진행형 게임", "scenes.json 장면 데이터와 선택지 분기 흐름을 이용해 사용자가 이야기의 방향을 고르는 구조", "Java Swing · JSON 장면 데이터", "src\\choice", "#ff8fcb", false, ""),
            new Project("escape", "탈출 게임", "Java 게임", "맵, 캐릭터, 아이템 이미지가 있는 탈출형 게임", "방 탐색과 아이템 확인 흐름을 중심으로 만든 데스크톱 게임 프로젝트", "Java Swing · 이미지 리소스", "src\\escape", "#ffd56a", false, ""),
            new Project("shooting", "슈팅 게임", "Java 게임", "키보드 조작으로 플레이하는 슈팅 게임", "플레이어 이동, 발사, 충돌, 점수 흐름을 확인할 수 있는 액션 게임 형태로 정리", "Java Swing · 키보드 이벤트 · Timer", "src\\shootingGame", "#a56dff", false, ""),
            new Project("sudoku", "스도쿠", "Java 게임", "숫자 퍼즐을 풀 수 있는 스도쿠 게임", "퍼즐 입력과 검증 흐름을 중심으로 숫자 배치 규칙을 연습할 수 있게 구성", "Java Swing · 퍼즐 로직", "src\\sudoku", "#8bdc9b", false, ""),
            new Project("yutnori", "윷놀이", "Java 게임", "이미지 리소스와 OpenAI 선택 로직이 포함된 윷놀이 게임", "말 이동, 턴 진행, OpenAI 응답과 기본 봇 fallback을 포함해 전통 보드게임 흐름을 구현", "Java Swing · OpenAI API · 이미지 리소스", "src\\yutnori", "#ffb86b", false, ""),
            new Project("calendar", "캘린더/가계부", "Java 앱", "일정과 예산을 관리하는 데스크톱 앱", "달력, 일정, 예산 입력을 한 화면에서 관리하고 JSON 파일로 저장/백업하는 개인 생산성 도구", "Java Swing · JSON 파일 저장", "src\\Calendar", "#7dd3fc", false, "")
    );

    public static void main(String[] args) throws IOException {
        createLaunchers();

        if (hasArg(args, "--export-static")) {
            exportStaticSite();
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        server.createContext("/", Main::handleHome);
        server.createContext("/run", Main::handleRun);
        server.createContext("/launchers", Main::handleLauncher);
        server.setExecutor(null);
        server.start();

        String url = "http://localhost:" + PORT + "/";

        boolean openBrowser = args.length == 0 || !"--no-browser".equals(args[0]);
        if (openBrowser && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(java.net.URI.create(url));
        }
    }

    private static boolean hasArg(String[] args, String expected) {
        for (String arg : args) {
            if (expected.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void exportStaticSite() throws IOException {
        Path index = ROOT.resolve("index.html");
        Files.writeString(index, buildHtml(), StandardCharsets.UTF_8);
        System.out.println("Static website exported: " + index);
        System.out.println("Deploy index.html with the launchers folder to a static web host.");
    }

    private static void createLaunchers() throws IOException {
        Files.createDirectories(LAUNCHER_DIR);

        for (Project project : PROJECTS) {
            Path launcher = launcherPath(project);
            String script = "dohwa".equals(project.id())
                    ? webLauncher(project)
                    : javaLauncher(project);
            Files.writeString(launcher, normalizeBatch(script), StandardCharsets.UTF_8);
        }
    }

    private static String webLauncher(Project project) {
        Path dohwaRoot = ROOT.resolve("src\\Dohwa").normalize();
        Path startScript = dohwaRoot.resolve("scripts\\start-dev.ps1");
        return """
                @echo off
                chcp 65001 > nul
                title yampf926 - %s
                echo.
                echo [%s]
                echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 함.
                echo.
                if not exist "%s" (
                    echo 프로젝트 폴더를 찾지 못함.
                    echo %s
                    echo.
                    echo 다른 기기에서 다운받은 배치파일은 실행할 수 없음.
                    pause
                    exit /b 1
                )
                if not exist "%s" (
                    echo Dohwa 실행 스크립트를 찾지 못함.
                    echo %s
                    pause
                    exit /b 1
                )
                echo %s 백엔드와 프론트엔드를 함께 실행함.
                echo 이미 8080 또는 412 포트에 이전 Dohwa 서버가 있으면 정리한 뒤 다시 시작함.
                echo 브라우저가 자동으로 열리지 않으면 http://localhost:412 으로 접속하면 됨.
                powershell -NoProfile -ExecutionPolicy Bypass -File "%s"
                start "" "http://localhost:412"
                if errorlevel 1 (
                    echo.
                    echo 실행 실패함. 위 오류 내용 확인 필요.
                )
                pause
                """.formatted(project.title(), project.title(), dohwaRoot, dohwaRoot, startScript, startScript, project.title(), startScript);
    }

    private static String javaLauncher(Project project) {
        Path directory = ROOT.resolve(project.folder()).normalize();
        String compileCommand = """
                powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = @(Get-ChildItem -Recurse -Path 'src' -Filter '*.java' | ForEach-Object { $_.FullName }); if ($files.Count -eq 0) { Write-Host 'Java 파일을 찾지 못함.'; exit 2 }; & javac -encoding UTF-8 -d out $files"
                """.strip();
        String classPath = "out;src;assets;images;data";

        if ("yutnori".equals(project.id())) {
            return yutnoriLauncher(project, directory);
        }

        return """
                @echo off
                chcp 65001 > nul
                title yampf926 - %s
                echo.
                echo [%s]
                echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 함.
                echo.
                if not exist "%s" (
                    echo 프로젝트 폴더를 찾지 못함.
                    echo %s
                    echo.
                    echo 다른 기기에서 다운받은 배치파일은 실행할 수 없음.
                    pause
                    exit /b 1
                )
                where java > nul 2> nul
                if errorlevel 1 (
                    echo java를 찾지 못함. JDK 설치 후 다시 실행하면 됨.
                    pause
                    exit /b 1
                )
                where javac > nul 2> nul
                if errorlevel 1 (
                    echo javac를 찾지 못함. JRE가 아니라 JDK가 필요함.
                    pause
                    exit /b 1
                )
                cd /d "%s"
                echo %s 컴파일 중...
                if not exist out mkdir out
                %s
                if errorlevel 1 (
                    echo.
                    echo 컴파일 실패함.
                    pause
                    exit /b 1
                )
                echo %s 실행 중...
                java -cp "%s" Main
                if errorlevel 1 (
                    echo.
                    echo 실행 실패함. 위 오류 내용 확인 필요.
                )
                pause
                """.formatted(project.title(), project.title(), directory, directory, directory, project.title(), compileCommand, project.title(), classPath);
    }

    private static String yutnoriLauncher(Project project, Path directory) {
        Path runScript = directory.resolve("run.bat");
        return """
                @echo off
                chcp 65001 > nul
                title yampf926 - %s
                echo.
                echo [%s]
                echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 함.
                echo.
                if not exist "%s" (
                    echo 프로젝트 폴더를 찾지 못함.
                    echo %s
                    echo.
                    echo 다른 기기에서 다운받은 배치파일은 실행할 수 없음.
                    pause
                    exit /b 1
                )
                if not exist "%s" (
                    echo 윷놀이 실행 스크립트를 찾지 못함.
                    echo %s
                    pause
                    exit /b 1
                )
                cd /d "%s"
                echo 개별 실행과 같은 run.bat으로 윷놀이를 실행함.
                call "%s"
                if errorlevel 1 (
                    echo.
                    echo 실행 실패함. 위 오류 내용 확인 필요.
                )
                pause
                """.formatted(project.title(), project.title(), directory, directory, runScript, runScript, directory, runScript);
    }

    private static String normalizeBatch(String script) {
        return script.stripIndent().replace("\r\n", "\n").replace("\n", "\r\n");
    }

    private static void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if ("/favicon.png".equals(path)) {
            sendFile(exchange, ROOT.resolve("favicon.png"), "image/png");
            return;
        }
        if (path.startsWith("/assets/")) {
            Path asset = ROOT.resolve(path.substring(1)).normalize();
            Path assetRoot = ROOT.resolve("assets").normalize();
            if (!asset.startsWith(assetRoot)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            sendFile(exchange, asset, contentType(asset));
            return;
        }

        send(exchange, 200, buildHtml(), "text/html; charset=UTF-8");
    }

    private static void handleRun(HttpExchange exchange) throws IOException {
        applyCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()) && !"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
            return;
        }

        String id = exchange.getRequestURI().getPath().replaceFirst("^/run/?", "");
        Project project = PROJECTS.stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElse(null);

        if (project == null) {
            send(exchange, 404, "프로젝트를 찾지 못함.", "text/plain; charset=UTF-8");
            return;
        }

        Path launcher = launcherPath(project);
        if (!Files.exists(launcher)) {
            createLaunchers();
        }

        new ProcessBuilder("cmd", "/c", "start", "", launcher.toString())
                .directory(ROOT.toFile())
                .start();

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 200, """
                    <!DOCTYPE html>
                    <html lang="ko">
                    <head>
                        <meta charset="UTF-8">
                        <title>프로젝트 실행</title>
                    </head>
                    <body>
                        <p>%s 실행 요청을 보냈음.</p>
                        <p>이 창은 닫아도 됨.</p>
                    </body>
                    </html>
                    """.formatted(project.title()), "text/html; charset=UTF-8");
            return;
        }

        send(exchange, 200, project.title() + " 실행 파일을 열었음.", "text/plain; charset=UTF-8");
    }

    private static void handleLauncher(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
            return;
        }

        String fileName = Path.of(exchange.getRequestURI().getPath()).getFileName().toString();
        if (!fileName.endsWith(".bat")) {
            send(exchange, 404, "파일을 찾지 못함.", "text/plain; charset=UTF-8");
            return;
        }

        Path launcher = LAUNCHER_DIR.resolve(fileName).normalize();
        if (!launcher.startsWith(LAUNCHER_DIR) || !Files.exists(launcher)) {
            send(exchange, 404, "파일을 찾지 못함.", "text/plain; charset=UTF-8");
            return;
        }

        byte[] bytes = Files.readAllBytes(launcher);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/x-bat; charset=UTF-8");
        headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static Path launcherPath(Project project) {
        return LAUNCHER_DIR.resolve(project.id() + ".bat");
    }

    private static String filterCategories(Project project) {
        List<String> categories = new ArrayList<>();
        if (project.webProject() || project.type().contains("웹")) {
            categories.add("web");
        }
        if (!project.webProject()) {
            categories.add("java");
        }
        if (project.type().contains("게임") || "game".equals(project.id())) {
            categories.add("game");
        }
        return String.join(" ", categories);
    }

    private static String buildHtml() {
        StringBuilder cards = new StringBuilder();
        for (Project project : PROJECTS) {
            cards.append("""
                    <article class="card" style="--project-color:%s" data-title="%s" data-type="%s" data-categories="%s">
                        <div class="thumb"><span>%s</span></div>
                        <div class="card-body">
                            <span class="tag">%s</span>
                            <h3>%s</h3>
                            <p>%s</p>
                            <div class="tech">%s</div>
                            <details class="project-detail">
                                <summary>상세 보기</summary>
                                <p>%s</p>
                            </details>
                            %s
                        </div>
                    </article>
                    """.formatted(
                    project.accent(),
                    project.title().toLowerCase(),
                    project.type().toLowerCase(),
                    filterCategories(project),
                    project.id().toUpperCase(),
                    project.type(),
                    project.title(),
                    project.description(),
                    project.tech(),
                    project.detail(),
                    actionHtml(project)
            ));
        }

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>박서영의 작업 공간</title>
                    <link rel="icon" type="image/png" href="favicon.png">
                    <link rel="shortcut icon" type="image/png" href="favicon.png">
                    <style>
                        * { box-sizing: border-box; }
                        :root {
                            --bg: #fffaff;
                            --panel: #ffffff;
                            --ink: #17202a;
                            --muted: #667085;
                            --line: #eadff8;
                            --lavender: #c9b8ff;
                            --lavender-soft: #f5f1ff;
                            --lavender-mid: #c9b8ff;
                            --lavender-deep: #8064d9;
                            --lavender-ink: #47366f;
                            --pink: #ff8fcb;
                            --aqua: #73d7ff;
                            --gold: #ffd56a;
                            --hover-accent: #8064d9;
                            --hover-accent-soft: #f5f1ff;
                            --shadow: 0 24px 70px rgba(117, 96, 181, 0.22);
                        }
                        body {
                            margin: 0;
                            min-height: 100vh;
                            background:
                                radial-gradient(circle at 12%% 8%%, rgba(201, 184, 255, 0.42), transparent 28%%),
                                radial-gradient(circle at 84%% 10%%, rgba(255, 143, 203, 0.22), transparent 26%%),
                                radial-gradient(circle at 72%% 68%%, rgba(115, 215, 255, 0.18), transparent 28%%),
                                radial-gradient(circle at 18%% 82%%, rgba(255, 213, 106, 0.15), transparent 24%%),
                                linear-gradient(180deg, #fffaff 0%%, #f8f2ff 48%%, #ffffff 100%%),
                                var(--bg);
                            color: var(--ink);
                            font-family: Arial, "Noto Sans KR", sans-serif;
                            word-break: keep-all;
                            overflow-wrap: break-word;
                            line-break: strict;
                        }
                        header {
                            position: relative;
                            min-height: 380px;
                            display: flex;
                            align-items: center;
                            color: white;
                            background:
                                linear-gradient(105deg, rgba(45, 30, 76, 0.92), rgba(112, 76, 186, 0.68) 58%%, rgba(255, 143, 203, 0.28)),
                                url("https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1800&q=80") center/cover;
                        }
                        header::after {
                            content: "";
                            position: absolute;
                            inset: auto 0 -1px 0;
                            height: 82px;
                            background: linear-gradient(180deg, transparent, var(--bg));
                            pointer-events: none;
                        }
                        .wrap {
                            width: min(1180px, calc(100%% - 44px));
                            margin: 0 auto;
                        }
                        nav {
                            position: sticky;
                            top: 0;
                            z-index: 10;
                            border-bottom: 1px solid rgba(223, 229, 236, 0.88);
                            background: rgba(255, 255, 255, 0.9);
                            backdrop-filter: blur(16px);
                        }
                        .nav-inner {
                            min-height: 68px;
                            padding: 12px 0;
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            gap: 18px;
                        }
                        .brand {
                            display: flex;
                            align-items: center;
                            gap: 12px;
                            font-weight: 900;
                            white-space: normal;
                        }
                        .brand::before {
                            content: "";
                            width: 34px;
                            height: 34px;
                            flex: 0 0 auto;
                            background: url("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA3MiA3MiI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJnIiB4MT0iMTAiIHkxPSI4IiB4Mj0iNjIiIHkyPSI2NCIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiPjxzdG9wIHN0b3AtY29sb3I9IiM4MDY0ZDkiLz48c3RvcCBvZmZzZXQ9Ii41OCIgc3RvcC1jb2xvcj0iI2M5YjhmZiIvPjxzdG9wIG9mZnNldD0iMSIgc3RvcC1jb2xvcj0iI2ZmOGZjYiIvPjwvbGluZWFyR3JhZGllbnQ+PGZpbHRlciBpZD0icyIgeD0iLTIwJSIgeT0iLTIwJSIgd2lkdGg9IjE0MCUiIGhlaWdodD0iMTQwJSI+PGZlRHJvcFNoYWRvdyBkeD0iMCIgZHk9IjUiIHN0ZERldmlhdGlvbj0iNCIgZmxvb2QtY29sb3I9IiM4MDY0ZDkiIGZsb29kLW9wYWNpdHk9Ii4yOCIvPjwvZmlsdGVyPjwvZGVmcz48cGF0aCBmaWx0ZXI9InVybCgjcykiIGZpbGw9InVybCgjZykiIGQ9Ik0xOCAxNEgyOEMyOCA3IDQwIDcgNDAgMTRINTRDNTcuMyAxNCA2MCAxNi43IDYwIDIwVjMwQzY3IDMwIDY3IDQyIDYwIDQyVjU0QzYwIDU3LjMgNTcuMyA2MCA1NCA2MEg0MEM0MCA1MyAyOCA1MyAyOCA2MEgxOEMxNC43IDYwIDEyIDU3LjMgMTIgNTRWNDJDMjAgNDIgMjAgMzAgMTIgMzBWMjBDMTIgMTYuNyAxNC43IDE0IDE4IDE0WiIvPjxwYXRoIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS13aWR0aD0iMyIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBvcGFjaXR5PSIuNTYiIGQ9Ik0xOCAxNEgyOEMyOCA3IDQwIDcgNDAgMTRINTRDNTcuMyAxNCA2MCAxNi43IDYwIDIwVjMwQzY3IDMwIDY3IDQyIDYwIDQyVjU0QzYwIDU3LjMgNTcuMyA2MCA1NCA2MEg0MEM0MCA1MyAyOCA1MyAyOCA2MEgxOEMxNC43IDYwIDEyIDU3LjMgMTIgNTRWNDJDMjAgNDIgMjAgMzAgMTIgMzBWMjBDMTIgMTYuNyAxNC43IDE0IDE4IDE0WiIvPjwvc3ZnPg==") center/contain no-repeat;
                        }
                        .hero {
                            max-width: 780px;
                            padding: 78px 0 96px;
                        }
                        .eyebrow {
                            display: inline-flex;
                            align-items: center;
                            min-height: 34px;
                            margin-bottom: 18px;
                            border: 1px solid rgba(255, 255, 255, 0.26);
                            border-radius: 999px;
                            padding: 7px 13px;
                            background: rgba(255, 255, 255, 0.12);
                            color: rgba(255, 255, 255, 0.9);
                            font-size: 13px;
                            font-weight: 900;
                        }
                        h1 {
                            margin: 0 0 18px;
                            font-size: clamp(40px, 6vw, 72px);
                            line-height: 1.08;
                            letter-spacing: 0;
                            overflow-wrap: normal;
                            word-break: keep-all;
                        }
                        .hero p {
                            margin: 0;
                            max-width: 680px;
                            color: rgba(255, 255, 255, 0.88);
                            font-size: 18px;
                            line-height: 1.75;
                            word-break: keep-all;
                        }
                        .hero-actions {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 12px;
                            margin-top: 28px;
                        }
                        .hero-actions a {
                            min-height: 44px;
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            border-radius: 8px;
                            padding: 11px 16px;
                            color: white;
                            background: linear-gradient(135deg, rgba(201, 184, 255, 0.34), rgba(255, 143, 203, 0.20));
                            border: 1px solid rgba(255, 255, 255, 0.28);
                            text-decoration: none;
                            font-size: 14px;
                            font-weight: 900;
                            transition: transform 180ms ease, background 180ms ease;
                        }
                        .hero-actions a:hover {
                            transform: translateY(-2px);
                            background: linear-gradient(135deg, rgba(255, 255, 255, 0.22), rgba(255, 143, 203, 0.28));
                        }
                        main {
                            padding: 48px 0 76px;
                        }
                        .content-section {
                            margin-bottom: 28px;
                            border: 1px solid rgba(223, 229, 236, 0.96);
                            border-radius: 8px;
                            padding: 22px;
                            background: rgba(255, 255, 255, 0.9);
                            box-shadow: 0 18px 48px rgba(117, 96, 181, 0.12);
                        }
                        .section-title {
                            display: flex;
                            align-items: flex-end;
                            justify-content: space-between;
                            gap: 20px;
                            margin-bottom: 24px;
                        }
                        h2 {
                            margin: 0 0 8px;
                            font-size: 30px;
                            line-height: 1.25;
                            letter-spacing: 0;
                        }
                        .section-title p {
                            max-width: 720px;
                            margin: 0;
                            color: var(--muted);
                            line-height: 1.7;
                            word-break: keep-all;
                        }
                        .toolbar {
                            display: grid;
                            grid-template-columns: minmax(220px, 1fr) auto auto;
                            gap: 14px;
                            align-items: center;
                            margin-bottom: 22px;
                            padding: 14px;
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 242, 255, 0.86));
                            box-shadow: 0 12px 28px rgba(117, 96, 181, 0.08);
                        }
                        .search {
                            width: 100%%;
                            min-height: 46px;
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            padding: 12px 14px;
                            color: var(--ink);
                            background: white;
                            font: inherit;
                            outline: none;
                        }
                        .search:focus {
                            border-color: var(--lavender-mid);
                            box-shadow: 0 0 0 4px rgba(201, 184, 255, 0.26);
                        }
                        .filters {
                            display: flex;
                            flex-wrap: wrap;
                            justify-content: flex-end;
                            gap: 8px;
                        }
                        .filter {
                            min-height: 38px;
                            border: 1px solid var(--line);
                            border-radius: 999px;
                            padding: 8px 12px;
                            color: var(--lavender-ink);
                            background: white;
                            font-size: 13px;
                            font-weight: 900;
                            cursor: pointer;
                            transition: transform 160ms ease, border-color 160ms ease, background 160ms ease;
                        }
                        .filter:hover {
                            transform: translateY(-1px);
                            border-color: var(--hover-accent);
                            background: var(--hover-accent-soft);
                        }
                        .filter.active {
                            border-color: var(--lavender-mid);
                            background: linear-gradient(135deg, var(--lavender-soft), #fff0fa);
                            color: var(--lavender-deep);
                        }
                        .reset-filter {
                            min-height: 38px;
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            padding: 8px 12px;
                            color: var(--lavender-ink);
                            background: linear-gradient(135deg, #ffffff, #f5f1ff);
                            font-size: 13px;
                            font-weight: 900;
                            cursor: pointer;
                        }
                        .result-meta {
                            margin: -10px 0 22px;
                            color: var(--muted);
                            font-size: 13px;
                            font-weight: 800;
                        }
                        .no-results {
                            display: none;
                            margin-top: 18px;
                            border: 1px dashed var(--lavender);
                            border-radius: 8px;
                            padding: 24px;
                            color: var(--lavender-ink);
                            background: rgba(255, 255, 255, 0.78);
                            text-align: center;
                            font-weight: 900;
                        }
                        .no-results.show {
                            display: block;
                        }
                        .stats {
                            display: grid;
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                            gap: 14px;
                            margin-bottom: 24px;
                        }
                        .stat {
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            padding: 16px;
                            background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 242, 255, 0.86));
                            box-shadow: 0 12px 28px rgba(117, 96, 181, 0.08);
                        }
                        .stat strong {
                            display: block;
                            color: var(--lavender-deep);
                            font-size: 26px;
                            line-height: 1.1;
                        }
                        .stat span {
                            display: block;
                            margin-top: 6px;
                            color: var(--muted);
                            font-size: 13px;
                            font-weight: 800;
                        }
                        .showcase {
                            display: grid;
                            grid-template-columns: 1.15fr 0.85fr;
                            gap: 20px;
                            align-items: stretch;
                        }
                        .feature,
                        .summary {
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            overflow: hidden;
                            background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 242, 255, 0.86));
                            box-shadow: 0 12px 28px rgba(117, 96, 181, 0.08);
                        }
                        .feature {
                            min-height: 300px;
                            display: grid;
                            grid-template-columns: minmax(0, 1fr) 220px;
                        }
                        .feature-copy {
                            padding: 26px;
                            display: grid;
                            align-content: center;
                            gap: 14px;
                        }
                        .feature-copy small {
                            width: fit-content;
                            border-radius: 999px;
                            padding: 7px 11px;
                            color: var(--lavender-deep);
                            background: #fff0fa;
                            font-weight: 900;
                        }
                        .feature-copy h2 {
                            margin: 0;
                            font-size: 34px;
                        }
                        .feature-copy p,
                        .summary p {
                            margin: 0;
                            color: var(--muted);
                            line-height: 1.72;
                            word-break: keep-all;
                        }
                        .feature-art {
                            min-height: 100%%;
                            background:
                                radial-gradient(circle at 72%% 24%%, rgba(255, 255, 255, 0.8) 0 11%%, transparent 12%%),
                                radial-gradient(circle at 26%% 74%%, rgba(255, 213, 106, 0.34) 0 13%%, transparent 14%%),
                                linear-gradient(150deg, #8064d9 0%%, #c9b8ff 48%%, #ff8fcb 100%%);
                        }
                        .summary {
                            padding: 22px;
                            display: grid;
                            align-content: center;
                            gap: 14px;
                        }
                        .summary-list {
                            display: grid;
                            gap: 10px;
                        }
                        .summary-item {
                            display: grid;
                            grid-template-columns: 38px minmax(0, 1fr);
                            gap: 12px;
                            align-items: center;
                            padding: 12px;
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            background: rgba(255, 255, 255, 0.88);
                        }
                        .summary-item span {
                            width: 38px;
                            height: 38px;
                            display: grid;
                            place-items: center;
                            border-radius: 8px;
                            color: white;
                            background: linear-gradient(135deg, var(--lavender-deep), var(--pink));
                            font-weight: 900;
                        }
                        .summary-item strong {
                            display: block;
                            margin-bottom: 3px;
                            color: var(--ink);
                        }
                        .summary-item em {
                            display: block;
                            color: var(--muted);
                            font-style: normal;
                            font-size: 13px;
                            line-height: 1.45;
                        }
                        .grid {
                            display: grid;
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                            gap: 20px;
                        }
                        .card {
                            --project-color: var(--lavender);
                            min-width: 0;
                            overflow: hidden;
                            display: flex;
                            flex-direction: column;
                            border: 1px solid rgba(223, 229, 236, 0.96);
                            border-radius: 8px;
                            background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 242, 255, 0.86));
                            box-shadow: 0 12px 28px rgba(117, 96, 181, 0.08);
                            transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
                        }
                        .card:hover {
                            transform: translateY(-7px) scale(1.01);
                            border-color: var(--hover-accent);
                            box-shadow: var(--shadow);
                        }
                        .card:hover .thumb {
                            filter: saturate(1.16) contrast(1.04);
                        }
                        .card:hover .thumb::after {
                            transform: translate(-4px, -4px) scale(1.08);
                        }
                        .thumb {
                            min-height: 128px;
                            position: relative;
                            overflow: hidden;
                            background:
                                radial-gradient(circle at 18%% 22%%, rgba(255, 255, 255, 0.82) 0 10%%, transparent 11%%),
                                radial-gradient(circle at 72%% 34%%, rgba(255, 255, 255, 0.48) 0 15%%, transparent 16%%),
                                linear-gradient(135deg, #7d5ce1 0%%, #c9b8ff 46%%, #ff8fcb 100%%);
                        }
                        .thumb::after {
                            content: "";
                            position: absolute;
                            right: 22px;
                            bottom: 18px;
                            width: 78px;
                            height: 78px;
                            border: 1px solid rgba(255, 255, 255, 0.42);
                            border-radius: 50%%;
                            box-shadow: -28px -18px 0 rgba(255, 255, 255, 0.16), -54px 8px 0 rgba(255, 213, 106, 0.22);
                            transition: transform 180ms ease;
                        }
                        .thumb span {
                            position: absolute;
                            left: 18px;
                            bottom: 16px;
                            z-index: 1;
                            color: white;
                            font-size: 22px;
                            line-height: 1;
                            font-weight: 900;
                            text-shadow: 0 8px 22px rgba(45, 30, 76, 0.34);
                        }
                        .card-body {
                            padding: 20px;
                            display: flex;
                            flex: 1;
                            flex-direction: column;
                        }
                        .tag {
                            width: fit-content;
                            max-width: 100%%;
                            margin-bottom: 13px;
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            padding: 7px 11px;
                            color: var(--lavender-deep);
                            background: linear-gradient(135deg, #ffffff, var(--lavender-soft));
                            font-size: 12px;
                            line-height: 1.25;
                            font-weight: 900;
                            overflow-wrap: break-word;
                        }
                        h3 {
                            margin: 0 0 10px;
                            font-size: 21px;
                            line-height: 1.32;
                            letter-spacing: 0;
                            word-break: keep-all;
                            overflow-wrap: break-word;
                        }
                        .card p {
                            margin: 0;
                            flex: 1;
                            color: var(--muted);
                            line-height: 1.68;
                            font-size: 14px;
                            word-break: keep-all;
                            overflow-wrap: break-word;
                        }
                        .tech {
                            margin-top: 14px;
                            border-top: 1px solid var(--line);
                            padding-top: 12px;
                            color: var(--lavender-ink);
                            font-size: 12px;
                            line-height: 1.45;
                            font-weight: 900;
                            overflow-wrap: break-word;
                        }
                        .project-detail {
                            margin-top: 12px;
                            color: var(--muted);
                            font-size: 13px;
                            line-height: 1.62;
                        }
                        .project-detail summary {
                            width: fit-content;
                            cursor: pointer;
                            color: var(--lavender-deep);
                            font-weight: 900;
                        }
                        .project-detail p {
                            margin-top: 8px;
                            flex: 0;
                        }
                        .card-actions {
                            display: grid;
                            grid-template-columns: 1fr;
                            gap: 10px;
                            margin-top: 20px;
                        }
                        button,
                        .card a {
                            min-width: 0;
                            min-height: 44px;
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            padding: 10px 12px;
                            background: linear-gradient(135deg, var(--lavender-deep), #a56dff 52%%, var(--pink));
                            color: white;
                            text-align: center;
                            text-decoration: none;
                            font: inherit;
                            font-size: 14px;
                            line-height: 1.25;
                            font-weight: 900;
                            cursor: pointer;
                            white-space: normal;
                            overflow-wrap: break-word;
                        }
                        button:disabled {
                            opacity: 0.58;
                            cursor: wait;
                        }
                        .card a {
                            border: 1px solid var(--line);
                            background: linear-gradient(135deg, #ffffff, #f5f1ff);
                            color: var(--lavender-ink);
                        }
                        .local-run,
                        .card a.local-run {
                            width: 100%%;
                            border: 0 !important;
                            outline: 0 !important;
                            background: linear-gradient(135deg, #8064d9 0%%, #9678e6 54%%, #a98bed 100%%) !important;
                            background-image: linear-gradient(135deg, #8064d9 0%%, #9678e6 54%%, #a98bed 100%%) !important;
                            color: white;
                            box-shadow: none !important;
                        }
                        button:hover,
                        .card a:hover,
                        .reset-filter:hover {
                            transform: translateY(-1px);
                            border-color: var(--hover-accent);
                            background: var(--hover-accent-soft);
                            color: var(--lavender-deep);
                            box-shadow: 0 10px 22px rgba(117, 96, 181, 0.18);
                        }
                        .local-run:hover,
                        .card a.local-run:hover {
                            border: 0 !important;
                            outline: 0 !important;
                            background: linear-gradient(135deg, #8064d9 0%%, #9678e6 54%%, #a98bed 100%%) !important;
                            background-image: linear-gradient(135deg, #8064d9 0%%, #9678e6 54%%, #a98bed 100%%) !important;
                            color: white;
                            box-shadow: none !important;
                        }
                        .note {
                            margin-top: 24px;
                            margin-bottom: 22px;
                            padding: 18px 20px;
                            border: 1px solid rgba(223, 229, 236, 0.96);
                            border-radius: 8px;
                            background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 242, 255, 0.86));
                            box-shadow: 0 12px 28px rgba(117, 96, 181, 0.08);
                            color: var(--muted);
                            line-height: 1.72;
                            word-break: keep-all;
                            overflow-wrap: break-word;
                        }
                        .music-panel {
                            width: min(220px, 100%%);
                            max-width: 100%%;
                            margin-top: 18px;
                            display: grid;
                            border: 1px solid rgba(255, 255, 255, 0.22);
                            border-radius: 8px;
                            padding: 6px;
                            background: rgba(255, 255, 255, 0.10);
                            backdrop-filter: blur(12px);
                        }
                        .music-frame {
                            width: 100%%;
                            aspect-ratio: 16 / 9;
                            border: 0;
                            border-radius: 6px;
                            background: rgba(255, 255, 255, 0.12);
                        }
                        .note-actions {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 10px;
                            margin-top: 14px;
                        }
                        .guide-button {
                            width: fit-content;
                            min-height: 38px;
                            border-radius: 8px;
                            padding: 8px 12px;
                            font-size: 13px;
                        }
                        .modal {
                            position: fixed;
                            inset: 0;
                            z-index: 30;
                            display: none;
                            align-items: center;
                            justify-content: center;
                            padding: 24px;
                            background: rgba(23, 32, 42, 0.48);
                        }
                        .modal.show {
                            display: flex;
                        }
                        .modal-panel {
                            width: min(520px, 100%%);
                            border-radius: 8px;
                            border: 1px solid var(--line);
                            background: white;
                            box-shadow: var(--shadow);
                            padding: 22px;
                        }
                        .modal-panel h2 {
                            margin-bottom: 12px;
                        }
                        .modal-panel ol {
                            margin: 0;
                            padding-left: 22px;
                            color: var(--muted);
                            line-height: 1.75;
                        }
                        .modal-panel button {
                            margin-top: 18px;
                        }
                        footer {
                            margin-top: 48px;
                            color: white;
                            background:
                                radial-gradient(circle at 16%% 18%%, rgba(255, 143, 203, 0.34), transparent 24%%),
                                linear-gradient(135deg, #2d1e4c, #8064d9 58%%, #a56dff);
                        }
                        .footer-inner {
                            width: min(1180px, calc(100%% - 44px));
                            margin: 0 auto;
                            display: grid;
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                            gap: 18px;
                            padding: 34px 0;
                        }
                        .footer-box h3 {
                            margin: 0 0 8px;
                        }
                        .footer-box p,
                        .footer-box a {
                            margin: 0;
                            color: rgba(255, 255, 255, 0.78);
                            line-height: 1.65;
                            text-decoration: none;
                            overflow-wrap: break-word;
                        }
                        .footer-box {
                            border-left: 1px solid rgba(255, 255, 255, 0.18);
                            padding-left: 16px;
                        }
                        .footer-bottom {
                            border-top: 1px solid rgba(255, 255, 255, 0.16);
                            padding: 14px 0 18px;
                            color: rgba(255, 255, 255, 0.62);
                            font-size: 13px;
                        }
                        @media (max-width: 1040px) {
                            .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
                            .showcase,
                            .feature { grid-template-columns: 1fr; }
                            .feature-art { min-height: 180px; }
                            .footer-inner { grid-template-columns: repeat(2, minmax(0, 1fr)); }
                        }
                        @media (max-width: 680px) {
                            .wrap { width: min(100%% - 28px, 1180px); }
                            .nav-inner,
                            .section-title {
                                align-items: flex-start;
                                flex-direction: column;
                            }
                            header { min-height: 340px; }
                            .hero { padding: 58px 0 72px; }
                            h1 { font-size: 38px; }
                            .music-panel {
                                width: min(220px, 100%%);
                            }
                            .toolbar,
                            .stats { grid-template-columns: 1fr; }
                            .filters { justify-content: flex-start; }
                            .grid { grid-template-columns: 1fr; }
                            .card-actions { grid-template-columns: 1fr; }
                            .footer-inner { grid-template-columns: 1fr; }
                            .footer-box {
                                border-left: 0;
                                padding-left: 0;
                            }
                        }
                    </style>
                </head>
                <body>
                    <nav>
                        <div class="wrap nav-inner">
                            <div class="brand">yampf926</div>
                        </div>
                    </nav>

                    <header>
                        <div class="wrap hero">
                            <div class="eyebrow">Published Portfolio</div>
                            <h1>박서영의 작업 공간</h1>
                            <p>웹으로 확인할 수 있는 작업과 Java로 직접 실행해볼 수 있는 프로젝트를 정리한 웹페이지</p>
                            <div class="music-panel" aria-label="배경 음악">
                                <iframe class="music-frame" src="https://www.youtube.com/embed/zD39SFYfKNE?rel=0" title="민기(MK) - I'll always love you" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
                            </div>
                            <div class="hero-actions">
                                <a href="#projects">프로젝트 보기</a>
                            </div>
                        </div>
                    </header>

                    <main class="wrap">
                        <section class="showcase content-section" aria-label="대표 작업">
                            <div class="feature">
                                <div class="feature-copy">
                                    <small>Featured</small>
                                    <h2>웹과 Java 프로젝트 허브</h2>
                                    <p>웹 프로젝트는 공개 페이지에서 바로 열고, Java 작업은 로컬 환경에서 실행할 수 있음.</p>
                                </div>
                                <div class="feature-art" aria-hidden="true"></div>
                            </div>
                            <div class="summary">
                                <h2>구성 요약</h2>
                                <div class="summary-list">
                                    <div class="summary-item"><span>01</span><div><strong>웹 프로젝트</strong><em>Dohwa와 브라우저 기반 보드게임 컬렉션</em></div></div>
                                    <div class="summary-item"><span>02</span><div><strong>Java 게임</strong><em>스토리, 탈출, 슈팅, 스도쿠, 윷놀이처럼 직접 실행하는 게임</em></div></div>
                                    <div class="summary-item"><span>03</span><div><strong>데스크톱 앱</strong><em>일정과 예산을 관리하는 캘린더/가계부 도구</em></div></div>
                                </div>
                            </div>
                        </section>

                        <section class="content-section" id="projects">
                            <div class="section-title">
                                <h2>프로젝트 모음</h2>
                            </div>
                            <div class="stats">
                                <div class="stat"><strong>8</strong><span>등록된 프로젝트</span></div>
                                <div class="stat"><strong>2</strong><span>웹 기반 작업</span></div>
                                <div class="stat"><strong>6</strong><span>Java 게임·앱</span></div>
                            </div>
                            <div class="toolbar">
                                <input class="search" id="search" type="search" placeholder="프로젝트 이름이나 기술 검색">
                                <div class="filters" aria-label="프로젝트 필터">
                                    <button class="filter active" type="button" data-filter="all">전체</button>
                                    <button class="filter" type="button" data-filter="web">웹</button>
                                    <button class="filter" type="button" data-filter="java">Java</button>
                                    <button class="filter" type="button" data-filter="game">게임</button>
                                </div>
                                <button class="reset-filter" id="resetFilter" type="button">초기화</button>
                            </div>
                            <div class="result-meta" id="resultMeta">전체 프로젝트 8개 표시 중.</div>
                            <div class="note">
                                Dohwa와 보드게임 컬렉션은 GitHub Pages 공개 주소로 바로 열림. Java 게임과 앱의 바로 실행은 이 PC에서 Main.java를 실행해 9260 포트의 로컬 실행 서버를 켠 뒤 사용할 수 있음.
                                <div class="note-actions">
                                    <button class="guide-button" id="openRunGuide" type="button">로컬 실행 방법</button>
                                </div>
                            </div>
                            <div class="grid">
                                __PROJECT_CARDS__
                            </div>
                            <div class="no-results" id="noResults">조건에 맞는 프로젝트 없음.</div>
                        </section>
                    </main>

                    <div class="modal" id="runGuide" role="dialog" aria-modal="true" aria-labelledby="runGuideTitle">
                        <div class="modal-panel">
                            <h2 id="runGuideTitle">로컬 실행 방법</h2>
                            <ol>
                                <li>Dohwa와 보드게임 컬렉션은 공개 페이지에서 바로 열림.</li>
                                <li>Java 게임과 앱은 IntelliJ에서 yampf926 프로젝트를 연 뒤 src/Main.java를 실행함.</li>
                                <li>로컬 실행 서버는 http://localhost:9260 에서만 열림.</li>
                                <li>Java 카드의 바로 실행 버튼을 누르면 이 서버가 내부 실행 파일을 실행함.</li>
                                <li>서버가 꺼져 있으면 Java 카드에서 localhost 연결 실패가 표시됨.</li>
                            </ol>
                            <button id="closeRunGuide" type="button">닫기</button>
                        </div>
                    </div>

                    <footer>
                        <div class="footer-inner">
                            <div class="footer-box">
                                <h3>Profile</h3>
                                <p>이름: 박서영</p>
                                <p>학력: 경인여자대학교 소프트웨어융합학과 졸업</p>
                            </div>
                            <div class="footer-box">
                                <h3>Contact</h3>
                                <p>Email: syp0426@naver.com</p>
                                <p>Phone: 010-9290-3442</p>
                            </div>
                            <div class="footer-box">
                                <h3>Links</h3>
                                <p><a href="https://github.com/yampf926?tab=repositories" target="_blank" rel="noreferrer">GitHub</a></p>
                            </div>
                        </div>
                        <div class="wrap footer-bottom">© 2026 박서영의 작업 공간. All rights reserved.</div>
                    </footer>

                    <script>
                        const search = document.querySelector("#search");
                        const cards = Array.from(document.querySelectorAll(".card"));
                        const filters = Array.from(document.querySelectorAll("[data-filter]"));
                        const resetFilter = document.querySelector("#resetFilter");
                        const resultMeta = document.querySelector("#resultMeta");
                        const noResults = document.querySelector("#noResults");
                        const runGuide = document.querySelector("#runGuide");
                        const openRunGuide = document.querySelector("#openRunGuide");
                        const closeRunGuide = document.querySelector("#closeRunGuide");
                        let activeFilter = "all";

                        function applyFilters() {
                            const query = search.value.trim().toLowerCase();
                            let visibleCount = 0;
                            cards.forEach((card) => {
                                const title = card.dataset.title || "";
                                const type = card.dataset.type || "";
                                const text = card.textContent.toLowerCase();
                                const categories = (card.dataset.categories || "").split(" ");
                                const matchesSearch = !query || title.includes(query) || type.includes(query) || text.includes(query);
                                const matchesFilter = activeFilter === "all" || categories.includes(activeFilter);
                                const visible = matchesSearch && matchesFilter;
                                card.hidden = !visible;
                                if (visible) {
                                    visibleCount++;
                                }
                            });
                            resultMeta.textContent = `총 ${cards.length}개 중 ${visibleCount}개 프로젝트 표시 중.`;
                            noResults.classList.toggle("show", visibleCount === 0);
                        }

                        search.addEventListener("input", applyFilters);
                        filters.forEach((filter) => {
                            filter.addEventListener("click", () => {
                                filters.forEach((item) => item.classList.remove("active"));
                                filter.classList.add("active");
                                activeFilter = filter.dataset.filter;
                                applyFilters();
                            });
                        });
                        resetFilter.addEventListener("click", () => {
                            search.value = "";
                            activeFilter = "all";
                            filters.forEach((item) => item.classList.toggle("active", item.dataset.filter === "all"));
                            applyFilters();
                        });

                        function isLocalAccess() {
                            const host = location.hostname;
                            return location.protocol === "file:"
                                || host === "localhost"
                                || host === "127.0.0.1"
                                || host === "::1"
                                || /^10\\./.test(host)
                                || /^192\\.168\\./.test(host)
                                || /^172\\.(1[6-9]|2\\d|3[0-1])\\./.test(host);
                        }

                        const canRunLocalProjects = isLocalAccess();
                        document.querySelectorAll("[data-run]").forEach((button) => {
                            button.addEventListener("click", async () => {
                                const id = button.dataset.run;
                                const runUrl = canRunLocalProjects ? `/run/${id}` : `http://localhost:9260/run/${id}`;
                                button.disabled = true;

                                try {
                                    const response = await fetch(runUrl, { method: "POST" });
                                    if (!response.ok) {
                                        throw new Error(`HTTP ${response.status}`);
                                    }
                                } catch (error) {
                                    console.error(error);
                                    alert("로컬 실행 서버에 연결하지 못했음. IntelliJ에서 src/Main.java를 먼저 실행한 뒤 다시 눌러야 함.");
                                } finally {
                                    button.disabled = false;
                                }
                            });
                        });
                        function toggleRunGuide(show) {
                            runGuide.classList.toggle("show", show);
                        }
                        openRunGuide.addEventListener("click", () => toggleRunGuide(true));
                        closeRunGuide.addEventListener("click", () => toggleRunGuide(false));
                        runGuide.addEventListener("click", (event) => {
                            if (event.target === runGuide) {
                                toggleRunGuide(false);
                            }
                        });
                        document.addEventListener("keydown", (event) => {
                            if (event.key === "Escape") {
                                toggleRunGuide(false);
                            }
                        });
                    </script>
                </body>
                </html>
                """.replace("%%", "%").replace("__PROJECT_CARDS__", cards.toString());
    }

    private static String actionHtml(Project project) {
        if (!project.liveUrl().isBlank()) {
            return """
                    <div class="card-actions">
                        <a class="local-run" href="%s" target="_blank" rel="noopener">바로 실행</a>
                    </div>
                    """.formatted(project.liveUrl());
        }

        return """
                <div class="card-actions">
                    <button class="local-run" type="button" data-run="%s">바로 실행</button>
                </div>
                """.formatted(project.id());
    }

    private static void send(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void applyCors(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        headers.set("Access-Control-Allow-Private-Network", "true");
    }

    private static void sendFile(HttpExchange exchange, Path file, String contentType) throws IOException {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "public, max-age=3600");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".m4a")) return "audio/mp4";
        if (name.endsWith(".ogg")) return "audio/ogg";
        if (name.endsWith(".wav")) return "audio/wav";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
