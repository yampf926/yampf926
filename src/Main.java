import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    private static final int PORT = 9260;
    private static final int MAX_PORT = 9270;
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAUNCHER_DIR = ROOT.resolve("launchers");

    private record Project(
            String id,
            String title,
            String type,
            String description,
            String folder,
            String accent,
            boolean webProject
    ) {
    }

    private static final List<Project> PROJECTS = List.of(
            new Project("dohwa", "Dohwa 웹사이트", "웹페이지", "React/Vite로 만든 웹 포트폴리오 프로젝트입니다.", "src\\Dohwa\\frontend", "#c9b8ff", true),
            new Project("game", "보드게임 컬렉션", "웹페이지", "Java 로컬 서버로 실행되고 브라우저에서 열리는 보드게임 컬렉션입니다.", "src\\Game", "#c9b8ff", false),
            new Project("choice", "비주얼 노벨 게임", "Java 게임", "이미지와 선택지를 사용하는 스토리 진행형 게임입니다.", "src\\choice", "#c9b8ff", false),
            new Project("escape", "탈출 게임", "Java 게임", "맵, 캐릭터, 아이템 이미지가 있는 탈출형 게임입니다.", "src\\escape", "#c9b8ff", false),
            new Project("shooting", "슈팅 게임", "Java 게임", "키보드 조작으로 플레이하는 슈팅 게임입니다.", "src\\shootingGame", "#c9b8ff", false),
            new Project("sudoku", "스도쿠", "Java 게임", "숫자 퍼즐을 풀 수 있는 스도쿠 게임입니다.", "src\\sudoku", "#c9b8ff", false),
            new Project("yutnori", "윷놀이", "Java 게임", "이미지 리소스와 AI 이동 로직이 포함된 윷놀이 게임입니다.", "src\\yutnori", "#c9b8ff", false),
            new Project("calendar", "캘린더/가계부", "Java 앱", "일정과 예산을 관리하는 데스크톱 앱입니다.", "src\\Calendar", "#c9b8ff", false)
    );

    public static void main(String[] args) throws IOException {
        createLaunchers();

        if (hasArg(args, "--export-static")) {
            exportStaticSite();
            return;
        }

        HttpServer server = createServer();
        server.createContext("/", Main::handleHome);
        server.createContext("/run", Main::handleRun);
        server.createContext("/launchers", Main::handleLauncher);
        server.setExecutor(null);
        server.start();

        int port = server.getAddress().getPort();
        String url = "http://localhost:" + port + "/";
        System.out.println("yampf926 포트폴리오 서버가 실행되었습니다.");
        System.out.println(url);
        for (String accessUrl : accessUrls(port)) {
            System.out.println(accessUrl);
        }

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

    private static HttpServer createServer() throws IOException {
        IOException lastError = null;
        for (int port = PORT; port <= MAX_PORT; port++) {
            try {
                return HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            } catch (IOException error) {
                lastError = error;
            }
        }
        throw lastError == null ? new IOException("No available port") : lastError;
    }

    private static void createLaunchers() throws IOException {
        Files.createDirectories(LAUNCHER_DIR);

        for (Project project : PROJECTS) {
            Path launcher = launcherPath(project);
            String script = project.webProject()
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
                echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 합니다.
                echo.
                if not exist "%s" (
                    echo 프로젝트 폴더를 찾지 못했습니다.
                    echo %s
                    echo.
                    echo 다른 기기에서 다운받은 배치파일은 실행할 수 없습니다.
                    pause
                    exit /b 1
                )
                if not exist "%s" (
                    echo Dohwa 실행 스크립트를 찾지 못했습니다.
                    echo %s
                    pause
                    exit /b 1
                )
                echo %s 백엔드와 프론트엔드를 함께 실행합니다.
                echo 이미 8080 또는 412 포트에 이전 Dohwa 서버가 있으면 정리한 뒤 다시 시작합니다.
                echo 브라우저가 자동으로 열리지 않으면 http://localhost:412 으로 접속하세요.
                powershell -NoProfile -ExecutionPolicy Bypass -File "%s"
                start "" "http://localhost:412"
                if errorlevel 1 (
                    echo.
                    echo 실행에 실패했습니다. 위 오류 내용을 확인하세요.
                )
                pause
                """.formatted(project.title(), project.title(), dohwaRoot, dohwaRoot, startScript, startScript, project.title(), startScript);
    }

    private static String javaLauncher(Project project) {
        Path directory = ROOT.resolve(project.folder()).normalize();
        String compileCommand = """
                powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = @(Get-ChildItem -Recurse -Path 'src' -Filter '*.java' | ForEach-Object { $_.FullName }); if ($files.Count -eq 0) { Write-Host 'Java 파일을 찾지 못했습니다.'; exit 2 }; & javac -encoding UTF-8 -d out $files"
                """.strip();
        String classPath = "out;src;assets;images;data";

        if ("yutnori".equals(project.id())) {
            compileCommand = "javac -encoding UTF-8 -d out head_Main.java";
            classPath = "out;.;assets;images;data";
        }

        return """
                @echo off
                chcp 65001 > nul
                title yampf926 - %s
                echo.
                echo [%s]
                echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 합니다.
                echo.
                if not exist "%s" (
                    echo 프로젝트 폴더를 찾지 못했습니다.
                    echo %s
                    echo.
                    echo 다른 기기에서 다운받은 배치파일은 실행할 수 없습니다.
                    pause
                    exit /b 1
                )
                where java > nul 2> nul
                if errorlevel 1 (
                    echo java를 찾지 못했습니다. JDK를 설치한 뒤 다시 실행하세요.
                    pause
                    exit /b 1
                )
                where javac > nul 2> nul
                if errorlevel 1 (
                    echo javac를 찾지 못했습니다. JRE가 아니라 JDK가 필요합니다.
                    pause
                    exit /b 1
                )
                cd /d "%s"
                echo %s 컴파일 중...
                if not exist out mkdir out
                %s
                if errorlevel 1 (
                    echo.
                    echo 컴파일에 실패했습니다.
                    pause
                    exit /b 1
                )
                echo %s 실행 중...
                java -cp "%s" Main
                if errorlevel 1 (
                    echo.
                    echo 실행에 실패했습니다. 위 오류 내용을 확인하세요.
                )
                pause
                """.formatted(project.title(), project.title(), directory, directory, directory, project.title(), compileCommand, project.title(), classPath);
    }

    private static String normalizeBatch(String script) {
        return script.stripIndent().replace("\r\n", "\n").replace("\n", "\r\n");
    }

    private static void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
            return;
        }

        send(exchange, 200, buildHtml(), "text/html; charset=UTF-8");
    }

    private static void handleRun(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
            return;
        }

        String id = exchange.getRequestURI().getPath().replaceFirst("^/run/?", "");
        Project project = PROJECTS.stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElse(null);

        if (project == null) {
            send(exchange, 404, "프로젝트를 찾지 못했습니다.", "text/plain; charset=UTF-8");
            return;
        }

        Path launcher = launcherPath(project);
        if (!Files.exists(launcher)) {
            createLaunchers();
        }

        new ProcessBuilder("cmd", "/c", "start", "", launcher.toString())
                .directory(ROOT.toFile())
                .start();

        send(exchange, 200, project.title() + " 실행 파일을 열었습니다.", "text/plain; charset=UTF-8");
    }

    private static void handleLauncher(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
            return;
        }

        String fileName = Path.of(exchange.getRequestURI().getPath()).getFileName().toString();
        if (!fileName.endsWith(".bat")) {
            send(exchange, 404, "파일을 찾지 못했습니다.", "text/plain; charset=UTF-8");
            return;
        }

        Path launcher = LAUNCHER_DIR.resolve(fileName).normalize();
        if (!launcher.startsWith(LAUNCHER_DIR) || !Files.exists(launcher)) {
            send(exchange, 404, "파일을 찾지 못했습니다.", "text/plain; charset=UTF-8");
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
                        <div class="thumb"></div>
                        <div class="card-body">
                            <span class="tag">%s</span>
                            <h3>%s</h3>
                            <p>%s</p>
                            <div class="card-actions">
                                <button type="button" data-run="%s">실행</button>
                                <a href="launchers/%s.bat">배치파일</a>
                            </div>
                        </div>
                    </article>
                    """.formatted(
                    project.accent(),
                    project.title().toLowerCase(),
                    project.type().toLowerCase(),
                    filterCategories(project),
                    project.type(),
                    project.title(),
                    project.description(),
                    project.id(),
                    project.id()
            ));
        }

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>박서영의 작업 공간</title>
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
                            overflow-wrap: keep-all;
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
                            background: rgba(255, 255, 255, 0.82);
                            box-shadow: 0 16px 44px rgba(117, 96, 181, 0.13);
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
                            border-color: var(--lavender-mid);
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
                            background:
                                linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(245, 241, 255, 0.86));
                            box-shadow: 0 14px 34px rgba(117, 96, 181, 0.09);
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
                            margin-bottom: 34px;
                            align-items: stretch;
                        }
                        .feature,
                        .summary {
                            border: 1px solid var(--line);
                            border-radius: 8px;
                            overflow: hidden;
                            background: rgba(255, 255, 255, 0.88);
                            box-shadow: 0 18px 48px rgba(117, 96, 181, 0.14);
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
                            background: linear-gradient(135deg, #ffffff, #f8f2ff);
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
                            background:
                                linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(252, 249, 255, 0.94));
                            box-shadow: 0 16px 42px rgba(117, 96, 181, 0.13);
                            transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
                        }
                        .card:hover {
                            transform: translateY(-7px) scale(1.01);
                            border-color: color-mix(in srgb, var(--project-color) 44%%, var(--line));
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
                            border-radius: 999px;
                            padding: 7px 11px;
                            color: var(--lavender-deep);
                            background: linear-gradient(135deg, color-mix(in srgb, var(--project-color) 22%%, white), #fff0fa);
                            font-size: 12px;
                            line-height: 1.25;
                            font-weight: 900;
                            overflow-wrap: anywhere;
                        }
                        h3 {
                            margin: 0 0 10px;
                            font-size: 21px;
                            line-height: 1.32;
                            letter-spacing: 0;
                            word-break: keep-all;
                            overflow-wrap: anywhere;
                        }
                        .card p {
                            margin: 0;
                            flex: 1;
                            color: var(--muted);
                            line-height: 1.68;
                            font-size: 14px;
                            word-break: keep-all;
                            overflow-wrap: anywhere;
                        }
                        .card-actions {
                            display: grid;
                            grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
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
                            border: 0;
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
                            overflow-wrap: anywhere;
                        }
                        button:disabled {
                            opacity: 0.58;
                            cursor: wait;
                        }
                        .card a {
                            border: 1px solid color-mix(in srgb, var(--project-color) 26%%, var(--line));
                            background: linear-gradient(135deg, #ffffff, #f5f1ff);
                            color: var(--lavender-ink);
                        }
                        button:hover,
                        .card a:hover,
                        .reset-filter:hover {
                            transform: translateY(-1px);
                            box-shadow: 0 10px 22px rgba(117, 96, 181, 0.18);
                        }
                        .note {
                            margin-top: 24px;
                            padding: 18px 20px;
                            border: 1px solid rgba(223, 229, 236, 0.96);
                            border-left: 4px solid var(--lavender);
                            border-radius: 8px;
                            background: rgba(255, 255, 255, 0.92);
                            color: var(--muted);
                            line-height: 1.72;
                            word-break: keep-all;
                            overflow-wrap: anywhere;
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
                            overflow-wrap: anywhere;
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
                            <div class="eyebrow">Published Project Portfolio</div>
                            <h1>박서영의 작업 공간</h1>
                            <p>웹사이트와 Java 프로젝트를 모아 정리한 개인 작업 공간입니다.</p>
                            <div class="hero-actions">
                                <a href="#projects">프로젝트 보기</a>
                            </div>
                        </div>
                    </header>

                    <main class="wrap">
                        <section class="showcase" aria-label="대표 작업">
                            <div class="feature">
                                <div class="feature-copy">
                                    <small>Featured</small>
                                    <h2>웹과 Java 프로젝트 허브</h2>
                                    <p>작업한 여러 프로젝트를 한 화면에서 탐색하고 실행할 수 있도록 정리했습니다.</p>
                                </div>
                                <div class="feature-art" aria-hidden="true"></div>
                            </div>
                            <div class="summary">
                                <h2>작업 구성</h2>
                                <div class="summary-list">
                                    <div class="summary-item"><span>01</span><div><strong>웹 프로젝트</strong><em>Dohwa와 브라우저 기반 보드게임 컬렉션</em></div></div>
                                    <div class="summary-item"><span>02</span><div><strong>게임</strong><em>스토리, 탈출, 슈팅, 스도쿠, 윷놀이 등 직접 실행하는 Java 게임</em></div></div>
                                    <div class="summary-item"><span>03</span><div><strong>앱</strong><em>일정과 예산을 관리하는 데스크톱 도구</em></div></div>
                                </div>
                            </div>
                        </section>

                        <section id="projects">
                            <div class="section-title">
                                <h2>프로젝트 실행</h2>
                            </div>
                            <div class="stats">
                                <div class="stat"><strong>8</strong><span>등록된 프로젝트</span></div>
                                <div class="stat"><strong>2</strong><span>웹사이트</span></div>
                                <div class="stat"><strong>6</strong><span>게임·앱 프로젝트</span></div>
                            </div>
                            <div class="toolbar">
                                <input class="search" id="search" type="search" placeholder="프로젝트 검색">
                                <div class="filters" aria-label="프로젝트 필터">
                                    <button class="filter active" type="button" data-filter="all">전체</button>
                                    <button class="filter" type="button" data-filter="web">웹</button>
                                    <button class="filter" type="button" data-filter="java">Java</button>
                                    <button class="filter" type="button" data-filter="game">게임</button>
                                </div>
                                <button class="reset-filter" id="resetFilter" type="button">초기화</button>
                            </div>
                            <div class="result-meta" id="resultMeta">전체 프로젝트 8개를 표시 중입니다.</div>
                            <div class="grid">
                                %s
                            </div>
                            <div class="no-results" id="noResults">조건에 맞는 프로젝트가 없습니다.</div>
                        </section>
                    </main>

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
                        let activeFilter = "all";

                        function applyFilters() {
                            const query = search.value.trim().toLowerCase();
                            let visibleCount = 0;
                            cards.forEach((card) => {
                                const title = card.dataset.title || "";
                                const type = card.dataset.type || "";
                                const categories = (card.dataset.categories || "").split(" ");
                                const matchesSearch = !query || title.includes(query) || type.includes(query);
                                const matchesFilter = activeFilter === "all" || categories.includes(activeFilter);
                                const visible = matchesSearch && matchesFilter;
                                card.hidden = !visible;
                                if (visible) {
                                    visibleCount++;
                                }
                            });
                            resultMeta.textContent = `총 ${cards.length}개 중 ${visibleCount}개 프로젝트를 표시 중입니다.`;
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
                            if (!canRunLocalProjects) {
                                button.textContent = "로컬 실행 전용";
                                button.title = "공개 웹사이트에서는 방문자 PC의 Java 프로젝트를 직접 실행하지 않습니다.";
                                button.disabled = true;
                                return;
                            }

                            button.addEventListener("click", async () => {
                                const id = button.dataset.run;
                                button.disabled = true;

                                try {
                                    await fetch(`/run/${id}`, { method: "POST" });
                                } catch (error) {
                                    console.error(error);
                                } finally {
                                    button.disabled = false;
                                }
                            });
                        });
                    </script>
                </body>
                </html>
                """.formatted(cards);
    }

    private static List<String> accessUrls(int port) {
        List<String> urls = new ArrayList<>();
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.getHostAddress().contains(":")) {
                        continue;
                    }
                    urls.add("http://" + address.getHostAddress() + ":" + port + "/");
                }
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return urls;
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
}
