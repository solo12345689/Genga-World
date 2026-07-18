package com.example.data.network

object DocsHtml {
    fun getHtml(port: Int): String {
        return """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MovieBox API Playground</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Fira+Code:wght@400;500&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-color: #090A10;
            --card-color: #141622;
            --primary-red: #E50914;
            --crimson: #B81D24;
            --amber: #FF8C00;
            --text-primary: #F5F5F7;
            --text-secondary: #9E9EAF;
            --terminal-bg: #0C0D14;
            --terminal-green: #39FF14;
            --border-color: rgba(158, 158, 175, 0.15);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--bg-color);
            color: var(--text-primary);
            line-height: 1.6;
            display: flex;
            height: 100vh;
            overflow: hidden;
        }

        /* Layout Structure */
        #sidebar {
            width: 320px;
            background-color: var(--card-color);
            border-right: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            flex-shrink: 0;
        }

        #main-content {
            flex-grow: 1;
            overflow-y: auto;
            padding: 32px;
            scroll-behavior: smooth;
        }

        /* Sidebar Design */
        .sidebar-header {
            padding: 24px;
            border-bottom: 1px solid var(--border-color);
        }

        .sidebar-header h1 {
            font-size: 20px;
            color: var(--text-primary);
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .sidebar-header .tag {
            background-color: var(--primary-red);
            color: white;
            font-size: 10px;
            padding: 2px 6px;
            border-radius: 4px;
            text-transform: uppercase;
            font-weight: 800;
        }

        .sidebar-subtitle {
            font-size: 11px;
            color: var(--amber);
            font-weight: 600;
            margin-top: 4px;
        }

        .endpoint-nav {
            flex-grow: 1;
            overflow-y: auto;
            padding: 16px 8px;
        }

        .nav-section-title {
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            color: var(--text-secondary);
            margin: 16px 12px 8px 12px;
            font-weight: 700;
        }

        .nav-item {
            display: flex;
            align-items: center;
            padding: 10px 12px;
            border-radius: 8px;
            color: var(--text-secondary);
            text-decoration: none;
            font-size: 13px;
            font-weight: 500;
            margin-bottom: 4px;
            transition: all 0.2s ease;
        }

        .nav-item:hover {
            background-color: rgba(255, 255, 255, 0.05);
            color: var(--text-primary);
        }

        .nav-item.active {
            background-color: rgba(229, 9, 20, 0.1);
            color: var(--text-primary);
            border-left: 3px solid var(--primary-red);
        }

        .nav-item .method-badge {
            font-size: 9px;
            font-weight: 700;
            padding: 2px 6px;
            border-radius: 4px;
            margin-right: 8px;
            min-width: 42px;
            text-align: center;
        }

        .method-get { background-color: rgba(57, 255, 20, 0.15); color: #39FF14; }
        .method-post { background-color: rgba(255, 140, 0, 0.15); color: #FF8C00; }

        /* Main Content Styling */
        .api-header {
            margin-bottom: 40px;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 24px;
        }

        .api-header h2 {
            font-size: 32px;
            font-weight: 800;
            margin-bottom: 8px;
        }

        .api-header p {
            color: var(--text-secondary);
            font-size: 15px;
        }

        .card {
            background-color: var(--card-color);
            border-radius: 16px;
            border: 1px solid var(--border-color);
            padding: 24px;
            margin-bottom: 32px;
        }

        .endpoint-block {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 16px;
        }

        .endpoint-path {
            font-family: 'Fira Code', monospace;
            font-size: 16px;
            font-weight: 500;
            background: rgba(255, 255, 255, 0.04);
            padding: 4px 12px;
            border-radius: 6px;
            border: 1px solid var(--border-color);
        }

        .description {
            color: var(--text-secondary);
            font-size: 14px;
            margin-bottom: 24px;
        }

        /* Form Controls */
        .playground-section {
            border-top: 1px solid var(--border-color);
            padding-top: 24px;
            margin-top: 24px;
        }

        .playground-title {
            font-size: 14px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 16px;
            color: var(--amber);
        }

        .field-row {
            display: flex;
            gap: 16px;
            margin-bottom: 16px;
            flex-wrap: wrap;
        }

        .field-group {
            flex: 1;
            min-width: 200px;
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .field-group label {
            font-size: 12px;
            color: var(--text-secondary);
            font-weight: 600;
        }

        .field-group input, .field-group textarea, .field-group select {
            background-color: var(--terminal-bg);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            padding: 10px 14px;
            color: var(--text-primary);
            font-family: 'Inter', sans-serif;
            font-size: 13px;
            outline: none;
            transition: border-color 0.2s ease;
        }

        .field-group input:focus, .field-group textarea:focus {
            border-color: var(--primary-red);
        }

        .field-group textarea {
            font-family: 'Fira Code', monospace;
            resize: vertical;
            height: 120px;
        }

        .btn-send {
            background-color: var(--primary-red);
            color: white;
            border: none;
            padding: 12px 24px;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: background-color 0.2s ease;
            margin-top: 8px;
        }

        .btn-send:hover {
            background-color: var(--crimson);
        }

        /* Code/Response Console */
        .response-container {
            margin-top: 20px;
            display: none;
        }

        .response-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: rgba(255, 255, 255, 0.02);
            padding: 8px 16px;
            border-radius: 8px 8px 0 0;
            border: 1px solid var(--border-color);
            border-bottom: none;
        }

        .status-badge {
            font-size: 11px;
            font-family: 'Fira Code', monospace;
            padding: 3px 8px;
            border-radius: 4px;
            font-weight: 600;
        }

        .status-success {
            background-color: rgba(57, 255, 20, 0.15);
            color: var(--terminal-green);
        }

        .status-error {
            background-color: rgba(229, 9, 20, 0.15);
            color: var(--primary-red);
        }

        .response-box {
            background-color: var(--terminal-bg);
            border: 1px solid var(--border-color);
            border-radius: 0 0 8px 8px;
            padding: 16px;
            max-height: 400px;
            overflow-y: auto;
            font-family: 'Fira Code', monospace;
            font-size: 12px;
            white-space: pre-wrap;
            word-break: break-all;
            color: #A8FFB2;
        }

        .response-box.error {
            color: #FF8888;
        }

        /* Security Table */
        .spec-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 16px;
        }

        .spec-table th, .spec-table td {
            text-align: left;
            padding: 12px;
            border-bottom: 1px solid var(--border-color);
            font-size: 13px;
        }

        .spec-table th {
            color: var(--text-secondary);
            font-weight: 600;
        }

        .copy-btn {
            background: transparent;
            border: 1px solid var(--border-color);
            color: var(--text-secondary);
            padding: 2px 8px;
            font-size: 11px;
            border-radius: 4px;
            cursor: pointer;
            margin-left: 8px;
        }

        .copy-btn:hover {
            color: var(--text-primary);
            border-color: var(--text-primary);
        }

        .close-sidebar-btn {
            position: absolute;
            right: 16px;
            top: 24px;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--border-color);
            color: var(--text-secondary);
            width: 28px;
            height: 28px;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            font-size: 14px;
            font-weight: 700;
            transition: all 0.2s ease;
        }

        .close-sidebar-btn:hover {
            color: var(--text-primary);
            background: rgba(255, 255, 255, 0.1);
            border-color: var(--primary-red);
        }

        .open-sidebar-btn {
            background-color: var(--card-color);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 8px 16px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            display: none;
            align-items: center;
            gap: 8px;
            transition: all 0.2s ease;
        }

        .open-sidebar-btn:hover {
            border-color: var(--primary-red);
            background-color: rgba(229, 9, 20, 0.05);
        }
    </style>
</head>
<body>

    <!-- SIDEBAR -->
    <div id="sidebar">
        <div class="sidebar-header" style="position: relative; padding-right: 50px;">
            <h1>MovieBox <span class="tag">BFF Mock</span></h1>
            <div class="sidebar-subtitle">Decompiled API Gateway v16.2.1</div>
            <button onclick="toggleSidebar()" class="close-sidebar-btn" title="Close Sidebar">✕</button>
        </div>
        <div class="endpoint-nav">
            <div class="nav-section-title">Security & Protocol</div>
            <a href="#handshake" class="nav-item active" onclick="activateNav(this)">
                <span class="method-badge method-get">KEY</span>Handshake Specs
            </a>

            <div class="nav-section-title">Preferences & Auth</div>
            <a href="#user-info" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-get">GET</span>/user-info
            </a>
            <a href="#request-otp" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-post">POST</span>/request-otp
            </a>
            <a href="#login" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-post">POST</span>/login
            </a>
            <a href="#register" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-post">POST</span>/register
            </a>
            <a href="#logout" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-post">POST</span>/logout
            </a>

            <div class="nav-section-title">Media Catalog</div>
            <a href="#trending" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-get">GET</span>/trending
            </a>
            <a href="#discovery" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-get">GET</span>/discovery
            </a>
            <a href="#search" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-get">GET</span>/search
            </a>
            <a href="#search-suggestions" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-get">GET</span>/search-suggestions
            </a>
            <a href="#detail" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-get">GET</span>/detail/{id}
            </a>
            <a href="#episodes" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-get">GET</span>/episodes/{id}
            </a>
            <a href="#stream" class="nav-item" onclick="activateNav(this)">
                <span class="method-badge method-get">GET</span>/stream/{id}
            </a>
        </div>
    </div>

    <!-- MAIN CONTENT -->
    <div id="main-content">
        
        <div class="api-header">
            <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 12px; flex-wrap: wrap;">
                <button id="open-sidebar-btn" onclick="toggleSidebar()" class="open-sidebar-btn" title="Open Sidebar">☰ Show Sidebar</button>
                <h2 style="margin: 0;">Mock Server API Documentation</h2>
            </div>
            <p>Direct HTTP interactions, playground controls, and schema validation specifications.</p>
        </div>

        <!-- 1. HANDSHAKE & SECURITY SPECS -->
        <div id="handshake" class="card">
            <h3 style="margin-bottom: 8px;">Handshake & Gateway Secrets</h3>
            <p class="description">Overview of the verified credentials embedded inside the application’s JVM and Native cryptographic layers.</p>
            
            <table class="spec-table">
                <thead>
                    <tr>
                        <th>Security Key Identifier</th>
                        <th>Decrypted Secret Key Value</th>
                        <th>Scope</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>Gateway Sign Secret</strong></td>
                        <td>
                            <code>76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O</code>
                            <button class="copy-btn" onclick="copyText('76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O')">Copy</button>
                        </td>
                        <td>HMAC-MD5 app signature resolution (x-tr-signature)</td>
                    </tr>
                    <tr>
                        <td><strong>WeFeed App Secret</strong></td>
                        <td>
                            <code>df70dbad6215444ca9e87ee1078cc681</code>
                            <button class="copy-btn" onclick="copyText('df70dbad6215444ca9e87ee1078cc681')">Copy</button>
                        </td>
                        <td>Used for validating internal WeFeed Mobile BFF operations</td>
                    </tr>
                    <tr>
                        <td><strong>Easter Egg Bypass Code</strong></td>
                        <td>
                            <code>or666</code>
                            <button class="copy-btn" onclick="copyText('or666')">Copy</button>
                        </td>
                        <td>Type in bypass dialog to self-unlock Developer Mode</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- 2. GET /user-info -->
        <div id="user-info" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-get" style="font-size:12px; padding: 4px 10px;">GET</span>
                <span class="endpoint-path">/user-info</span>
            </div>
            <p class="description">Returns current local user authorization status, nickname, avatar, VIP status, or guest authorization defaults.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <button class="btn-send" onclick="sendTest('/user-info', 'GET', 'user-info')">Send Request</button>
                
                <div id="res-user-info" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-user-info" class="status-badge status-success">HTTP 200</span>
                    </div>
                    <pre id="box-user-info" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 3. POST /request-otp -->
        <div id="request-otp" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-post" style="font-size:12px; padding: 4px 10px;">POST</span>
                <span class="endpoint-path">/request-otp</span>
            </div>
            <p class="description">Asynchronously generates and requests verification OTP tokens for emails or mobile accounts from server.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <div class="field-row">
                    <div class="field-group">
                        <label>Account Target (Email or Phone Number)</label>
                        <input type="text" id="otp-account" value="test@example.com">
                    </div>
                    <div class="field-group">
                        <label>Auth Code Type (1 for Email, 0 for SMS)</label>
                        <select id="otp-type">
                            <option value="1">1 (Email OTP)</option>
                            <option value="0">0 (SMS Phone OTP)</option>
                        </select>
                    </div>
                </div>
                
                <button class="btn-send" onclick="sendOtpRequest()">Send Request</button>
                
                <div id="res-request-otp" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-request-otp" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-request-otp" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 4. POST /login -->
        <div id="login" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-post" style="font-size:12px; padding: 4px 10px;">POST</span>
                <span class="endpoint-path">/login</span>
            </div>
            <p class="description">Verifies client OTP and establishes a persistent OAuth token session.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <div class="field-row">
                    <div class="field-group">
                        <label>Account Target</label>
                        <input type="text" id="login-account" value="test@example.com">
                    </div>
                    <div class="field-group">
                        <label>One-Time Verification OTP (OTP is 1234 by default!)</label>
                        <input type="text" id="login-otp" value="1234">
                    </div>
                </div>
                
                <button class="btn-send" onclick="sendLoginRequest()">Send Request</button>
                
                <div id="res-login" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-login" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-login" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 5. POST /register -->
        <div id="register" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-post" style="font-size:12px; padding: 4px 10px;">POST</span>
                <span class="endpoint-path">/register</span>
            </div>
            <p class="description">Manually registers a new account details in preferences storage.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <div class="field-row">
                    <div class="field-group">
                        <label>Account Target (Email or Phone Number)</label>
                        <input type="text" id="reg-account" value="test@example.com">
                    </div>
                    <div class="field-group">
                        <label>Password</label>
                        <input type="password" id="reg-password" value="123456">
                    </div>
                    <div class="field-group">
                        <label>One-Time Verification OTP (OTP is 1234 by default!)</label>
                        <input type="text" id="reg-otp" value="1234">
                    </div>
                </div>
                
                <button class="btn-send" onclick="sendRegisterRequest()">Send Request</button>
                
                <div id="res-register" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-register" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-register" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 6. POST /logout -->
        <div id="logout" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-post" style="font-size:12px; padding: 4px 10px;">POST</span>
                <span class="endpoint-path">/logout</span>
            </div>
            <p class="description">Invalidates local shared auth session preferences and reverts current state to Guest profile.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <button class="btn-send" onclick="sendTest('/logout', 'POST', 'logout', '{}')">Send Request</button>
                
                <div id="res-logout" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-logout" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-logout" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 7. GET /trending -->
        <div id="trending" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-get" style="font-size:12px; padding: 4px 10px;">GET</span>
                <span class="endpoint-path">/trending</span>
            </div>
            <p class="description">Fetches current global trending and popular cinematic shows list resolved from high-tier BFF APIs.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <button class="btn-send" onclick="sendTest('/trending', 'GET', 'trending')">Send Request</button>
                
                <div id="res-trending" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-trending" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-trending" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 8. GET /discovery -->
        <div id="discovery" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-get" style="font-size:12px; padding: 4px 10px;">GET</span>
                <span class="endpoint-path">/discovery</span>
            </div>
            <p class="description">Resolves the curated landing layout groupings of high-rating movies and custom series feeds.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <button class="btn-send" onclick="sendTest('/discovery', 'GET', 'discovery')">Send Request</button>
                
                <div id="res-discovery" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-discovery" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-discovery" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 9. GET /search -->
        <div id="search" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-get" style="font-size:12px; padding: 4px 10px;">GET</span>
                <span class="endpoint-path">/search</span>
            </div>
            <p class="description">Executes unified library search queries across high-resolution titles.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <div class="field-row">
                    <div class="field-group">
                        <label>Search Keyword (q)</label>
                        <input type="text" id="search-q" value="Avenger">
                    </div>
                    <div class="field-group">
                        <label>Page Number</label>
                        <input type="number" id="search-page" value="1">
                    </div>
                </div>
                
                <button class="btn-send" onclick="sendSearchRequest()">Send Request</button>
                
                <div id="res-search" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-search" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-search" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 10. GET /search-suggestions -->
        <div id="search-suggestions" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-get" style="font-size:12px; padding: 4px 10px;">GET</span>
                <span class="endpoint-path">/search-suggestions</span>
            </div>
            <p class="description">Pulls top trending search terms dynamically from the live WeFeed search suggestions api.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <button class="btn-send" onclick="sendTest('/search-suggestions', 'GET', 'search-suggestions')">Send Request</button>
                
                <div id="res-search-suggestions" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-search-suggestions" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-search-suggestions" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 11. GET /detail/{id} -->
        <div id="detail" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-get" style="font-size:12px; padding: 4px 10px;">GET</span>
                <span class="endpoint-path">/detail/{subjectId}</span>
            </div>
            <p class="description">Resolves detail record schemas, poster assets, languages/dubs, casting members, and structural classifications.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <div class="field-row">
                    <div class="field-group">
                        <label>Subject ID</label>
                        <input type="text" id="detail-id" value="15632">
                    </div>
                </div>
                
                <button class="btn-send" onclick="sendDetailRequest()">Send Request</button>
                
                <div id="res-detail" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-detail" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-detail" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 12. GET /episodes/{id} -->
        <div id="episodes" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-get" style="font-size:12px; padding: 4px 10px;">GET</span>
                <span class="endpoint-path">/episodes/{seriesId}</span>
            </div>
            <p class="description">Resolves the multi-season directory structure mapping and listing episodic indexes for series.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <div class="field-row">
                    <div class="field-group">
                        <label>Series ID (Subject ID of TV Show)</label>
                        <input type="text" id="episodes-id" value="43128">
                    </div>
                </div>
                
                <button class="btn-send" onclick="sendEpisodesRequest()">Send Request</button>
                
                <div id="res-episodes" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-episodes" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-episodes" class="response-box"></pre>
                </div>
            </div>
        </div>

        <!-- 13. GET /stream/{id} -->
        <div id="stream" class="card">
            <div class="endpoint-block">
                <span class="method-badge method-get" style="font-size:12px; padding: 4px 10px;">GET</span>
                <span class="endpoint-path">/stream/{subjectId}</span>
            </div>
            <p class="description">Resolves video play sources, media links, and dynamic CloudFront decrypt cookies.</p>
            
            <div class="playground-section">
                <div class="playground-title">Playground Console</div>
                <div class="field-row">
                    <div class="field-group">
                        <label>Subject ID</label>
                        <input type="text" id="stream-id" value="15632">
                    </div>
                    <div class="field-group">
                        <label>Season (For TV shows)</label>
                        <input type="text" id="stream-se" value="1">
                    </div>
                    <div class="field-group">
                        <label>Episode (For TV Shows)</label>
                        <input type="text" id="stream-ep" value="1">
                    </div>
                </div>
                
                <button class="btn-send" onclick="sendStreamRequest()">Send Request</button>
                
                <div id="res-stream" class="response-container">
                    <div class="response-header">
                        <span>Response Object</span>
                        <span id="status-stream" class="status-badge">HTTP 200</span>
                    </div>
                    <pre id="box-stream" class="response-box"></pre>
                </div>
            </div>
        </div>

    </div>

    <!-- JS INTERACTIONS -->
    <script>
        const hostUrl = window.location.origin;

        function toggleSidebar() {
            const sidebar = document.getElementById('sidebar');
            const openBtn = document.getElementById('open-sidebar-btn');
            if (sidebar.style.display === 'none') {
                sidebar.style.display = 'flex';
                openBtn.style.display = 'none';
            } else {
                sidebar.style.display = 'none';
                openBtn.style.display = 'inline-flex';
            }
        }

        function copyText(val) {
            navigator.clipboard.writeText(val);
            alert("Copied to clipboard!");
        }

        function activateNav(el) {
            document.querySelectorAll('.nav-item').forEach(item => {
                item.classList.remove('active');
            });
            el.classList.add('active');
        }

        function showResult(boxId, statusId, containerId, status, data, isError=false) {
            const container = document.getElementById(containerId);
            const box = document.getElementById(boxId);
            const statusBadge = document.getElementById(statusId);

            container.style.display = "block";
            statusBadge.innerText = "HTTP " + status;
            
            if (status >= 200 && status < 300) {
                statusBadge.className = "status-badge status-success";
                box.className = "response-box";
            } else {
                statusBadge.className = "status-badge status-error";
                box.className = "response-box error";
            }

            if (isError) {
                box.innerText = data;
            } else {
                box.innerText = JSON.stringify(data, null, 2);
            }
        }

        async function sendTest(endpoint, method, id, bodyData=null) {
            const containerId = "res-" + id;
            const statusId = "status-" + id;
            const boxId = "box-" + id;
            
            try {
                const options = {
                    method: method,
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    }
                };
                if (bodyData) {
                    options.body = bodyData;
                }

                const response = await fetch(hostUrl + endpoint, options);
                const status = response.status;
                const txt = await response.text();
                let isJson = false;
                let data = txt;
                try {
                    data = JSON.parse(txt);
                    isJson = true;
                } catch (e) {
                    // Not JSON
                }
                showResult(boxId, statusId, containerId, status, data, !isJson);
            } catch (error) {
                showResult(boxId, statusId, containerId, 500, "Error connecting to local server: " + error.message, true);
            }
        }

        function sendOtpRequest() {
            const account = document.getElementById('otp-account').value;
            const typeVal = parseInt(document.getElementById('otp-type').value, 10);
            const payload = JSON.stringify({ account: account, type: typeVal });
            sendTest('/request-otp', 'POST', 'request-otp', payload);
        }

        function sendLoginRequest() {
            const account = document.getElementById('login-account').value;
            const password = document.getElementById('login-otp').value;
            const payload = JSON.stringify({ account: account, password: password });
            sendTest('/login', 'POST', 'login', payload);
        }

        function sendRegisterRequest() {
            const account = document.getElementById('reg-account').value;
            const password = document.getElementById('reg-password').value;
            const otp = document.getElementById('reg-otp').value;
            const payload = JSON.stringify({ account: account, password: password, otp: otp });
            sendTest('/register', 'POST', 'register', payload);
        }

        function sendSearchRequest() {
            const q = encodeURIComponent(document.getElementById('search-q').value);
            const page = parseInt(document.getElementById('search-page').value, 10) || 1;
            sendTest('/search?q=' + q + '&page=' + page, 'GET', 'search');
        }

        function sendDetailRequest() {
            const id = document.getElementById('detail-id').value;
            sendTest('/detail/' + id, 'GET', 'detail');
        }

        function sendEpisodesRequest() {
            const id = document.getElementById('episodes-id').value;
            sendTest('/episodes/' + id, 'GET', 'episodes');
        }

        function sendStreamRequest() {
            const id = document.getElementById('stream-id').value;
            const se = document.getElementById('stream-se').value;
            const ep = document.getElementById('stream-ep').value;
            sendTest('/stream/' + id + '?season=' + se + '&episode=' + ep, 'GET', 'stream');
        }
    </script>
</body>
</html>
""";
    }
}
