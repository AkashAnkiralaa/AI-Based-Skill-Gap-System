<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Skill Gap Analysis - Login</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        .login-container {
            background: white;
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
            width: 100%;
            max-width: 400px;
            padding: 40px;
        }

        .logo-section {
            text-align: center;
            margin-bottom: 30px;
        }

        .logo-section h1 {
            color: #667eea;
            font-size: 28px;
            margin-bottom: 10px;
        }

        .logo-section p {
            color: #666;
            font-size: 14px;
        }

        .form-group {
            margin-bottom: 20px;
        }

        .form-group label {
            display: block;
            color: #333;
            font-weight: 600;
            margin-bottom: 8px;
            font-size: 14px;
        }

        .form-group input {
            width: 100%;
            padding: 12px 15px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
            transition: border-color 0.3s;
        }

        .form-group input:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 5px rgba(102, 126, 234, 0.5);
        }

        .error-message {
            color: #e74c3c;
            font-size: 13px;
            margin-top: 10px;
            display: none;
        }

        .error-message.show {
            display: block;
        }

        .login-button {
            width: 100%;
            padding: 12px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
            margin-top: 20px;
        }

        .login-button:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
        }

        .login-button:active {
            transform: translateY(0);
        }

        .info-section {
            background: #f0f4ff;
            border-left: 4px solid #667eea;
            padding: 15px;
            border-radius: 5px;
            margin-top: 20px;
            font-size: 12px;
            color: #555;
            line-height: 1.6;
        }

        .demo-credentials {
            margin-top: 15px;
            padding-top: 15px;
            border-top: 1px solid #ddd;
        }

        .demo-credentials strong {
            color: #667eea;
            display: block;
            margin-bottom: 8px;
        }

        .credential-item {
            background: white;
            padding: 8px;
            margin: 5px 0;
            border-radius: 3px;
            font-size: 11px;
            font-family: monospace;
            color: #333;
        }

        .loading {
            display: none;
            text-align: center;
            margin-top: 20px;
        }

        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 0 auto;
        }

        @keyframes spin {
            0% {
                transform: rotate(0deg);
            }

            100% {
                transform: rotate(360deg);
            }
        }
    </style>
</head>

<body>
    <div class="login-container">
        <div id="errorMessage" class="error-message"></div>

        <div class="logo-section">
            <h1>🎯 Skill Gap Analysis</h1>
            <p>Student Portal - Login</p>
        </div>

        <form id="loginForm" method="POST" action="login">
            <div class="form-group">
                <label for="username">Email Address</label>
                <input type="email" id="username" name="username" placeholder="e.g., 24EG105K01@anurag.edu.in" required
                    autofocus>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" placeholder="Enter your password" required>
            </div>

            <button type="submit" class="login-button">Login</button>
            <div class="loading" id="loading">
                <div class="spinner"></div>
            </div>
        </form>

    </div>

        <script>
            // Check for error in URL
            const urlParams = new URLSearchParams(window.location.search);
            const errorParam = urlParams.get('error');
            if (errorParam) {
                const errorMsg = document.getElementById('errorMessage');
                errorMsg.textContent = errorParam;
                errorMsg.classList.add('show');
                window.history.replaceState({}, document.title, window.location.pathname);
            }

            document.getElementById('loginForm').addEventListener('submit', async function (e) {
                e.preventDefault();

                const username = document.getElementById('username').value;
                const password = document.getElementById('password').value;
                const errorMsg = document.getElementById('errorMessage');
                const loading = document.getElementById('loading');

                // Validate form
                if (!username || !password) {
                    errorMsg.textContent = 'Please fill in all fields.';
                    errorMsg.classList.add('show');
                    return;
                }

                // Show loading
                loading.style.display = 'block';
                errorMsg.classList.remove('show');

                // Submit form
                setTimeout(() => {
                    this.submit();
                }, 300);
            });
        </script>
</body>

</html>