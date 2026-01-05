<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>

<body>
<div class="main-container">
  <div class="login-container">
    <!-- Left Side - Branding -->
    <div class="branding-section">
      <div class="branding-content">
        <div class="logo-section">
          <div class="logo-circle">
            <img src="${url.resourcesPath}/img/log.png" alt="Logo Sindibad Group" class="logo-img"/>
          </div>
          <h3 class="company-name">Sindibad Group</h3>
          <p class="company-subtitle">Helpdesk Platform</p>
        </div>
        <div class="icon-wrapper">
          <div class="icon-circle">
            <span class="icon-emoji">🎧</span>
          </div>
        </div>
      </div>
      <div class="quote-section">
        <p class="quote-text">"Enabling seamless support interactions for better customer experiences."</p>
      </div>
    </div>

    <!-- Right Side - Login Form -->
    <div class="form-section">
      <div class="form-wrapper">
        <h2 class="form-title">Welcome Back!</h2>
        <p class="form-subtitle">Please login to your account</p>

        <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="alert alert-${message.type}">
                <#if message.type = 'success'><span class="alert-icon">✓</span></#if>
                <#if message.type = 'error'><span class="alert-icon">✗</span></#if>
                <span>${kcSanitize(message.summary)?no_esc}</span>
            </div>
        </#if>

        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
          <!-- Username/Email field -->
          <div class="form-group">
            <label for="username" class="form-label">
                <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>Email</#if>
            </label>
            <div class="input-wrapper">
              <div class="input-icon">
                <svg xmlns="http://www.w3.org/2000/svg" class="icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <input 
                id="username"
                type="text" 
                name="username"
                value="${(login.username!'')}"
                placeholder="Enter your email" 
                class="form-input" 
                required 
                autofocus
                autocomplete="off"
              />
            </div>
          </div>

          <!-- Password field -->
          <div class="form-group">
            <div class="label-row">
              <label for="password" class="form-label">Password</label>
              <#if realm.resetPasswordAllowed>
                <a href="${url.loginResetCredentialsUrl}" class="forgot-link">Forgot Password?</a>
              </#if>
            </div>
            <div class="input-wrapper">
              <div class="input-icon">
                <svg xmlns="http://www.w3.org/2000/svg" class="icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
              </div>
              <input 
                id="password"
                type="password" 
                name="password"
                placeholder="Enter your password" 
                class="form-input password-input" 
                required 
                autocomplete="off"
              />
              <button 
                type="button" 
                onclick="togglePassword()"
                class="password-toggle"
              >
                <svg id="eye-open" xmlns="http://www.w3.org/2000/svg" class="icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg id="eye-closed" xmlns="http://www.w3.org/2000/svg" class="icon" fill="none" viewBox="0 0 24 24" stroke="currentColor" style="display: none;">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Remember me -->
          <#if realm.rememberMe && !usernameEditDisabled??>
          <div class="remember-me">
            <input type="checkbox" id="rememberMe" name="rememberMe" class="checkbox" <#if login.rememberMe??>checked</#if>/>
            <label for="rememberMe" class="checkbox-label">Remember me</label>
          </div>
          </#if>

          <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

          <!-- Login button -->
          <button 
            type="submit"
            name="login"
            id="kc-login"
            class="submit-button"
          >
            <span>Login</span>
          </button>
        </form>

        <!-- Support link -->
        <div class="support-link">
          Need Help? <a href="#" class="link">Contact Support</a>
        </div>
      </div>
    </div>
  </div>
</div>

<script>
function togglePassword() {
    var passwordInput = document.getElementById('password');
    var eyeOpen = document.getElementById('eye-open');
    var eyeClosed = document.getElementById('eye-closed');
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        eyeOpen.style.display = 'none';
        eyeClosed.style.display = 'block';
    } else {
        passwordInput.type = 'password';
        eyeOpen.style.display = 'block';
        eyeClosed.style.display = 'none';
    }
}
</script>
</body>
</html>
