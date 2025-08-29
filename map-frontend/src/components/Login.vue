<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">
          <span class="logo-text">KINDD</span>
        </h1>
        <p class="login-subtitle">Provider Management Portal</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div v-if="error" class="alert alert-danger">
          <i class="bi bi-exclamation-circle me-2"></i>
          {{ error }}
        </div>

        <div class="form-group">
          <label for="username" class="form-label">
            <i class="bi bi-person-fill me-2"></i>Username
          </label>
          <input
            type="text"
            id="username"
            v-model="credentials.username"
            class="form-control"
            placeholder="Enter your username"
            required
            :disabled="loading"
            @input="clearError"
          />
        </div>

        <div class="form-group">
          <label for="password" class="form-label">
            <i class="bi bi-lock-fill me-2"></i>Password
          </label>
          <input
            type="password"
            id="password"
            v-model="credentials.password"
            class="form-control"
            placeholder="Enter your password"
            required
            :disabled="loading"
            @input="clearError"
          />
        </div>

        <div class="form-group">
          <div class="form-check">
            <input
              type="checkbox"
              id="rememberMe"
              v-model="rememberMe"
              class="form-check-input"
            />
            <label for="rememberMe" class="form-check-label"> Remember me </label>
          </div>
        </div>

        <button
          type="submit"
          class="btn btn-primary w-100"
          :disabled="loading || !credentials.username || !credentials.password"
        >
          <span v-if="loading">
            <span class="spinner-border spinner-border-sm me-2" role="status">
              <span class="visually-hidden">Loading...</span>
            </span>
            Signing in...
          </span>
          <span v-else>
            <i class="bi bi-box-arrow-in-right me-2"></i>
            Sign In
          </span>
        </button>
      </form>

      <div class="login-footer">
        <p class="text-muted small">
          <i class="bi bi-shield-lock me-1"></i>
          Secure access for authorized users only
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { authService } from "@/services/auth";

export default {
  name: "Login",

  data() {
    return {
      credentials: {
        username: "",
        password: "",
      },
      rememberMe: false,
      loading: false,
      error: null,
    };
  },

  mounted() {
    // Check if already authenticated
    if (authService.isAuthenticated()) {
      this.$router.push(this.$route.query.redirect || "/providers");
    }
  },

  methods: {
    async handleLogin() {
      this.loading = true;
      this.error = null;

      try {
        await authService.login(
          this.credentials.username,
          this.credentials.password,
          this.rememberMe
        );

        // Redirect to intended page or providers
        const redirectTo = this.$route.query.redirect || "/providers";
        this.$router.push(redirectTo);
      } catch (error) {
        console.error("Login error:", error);
        this.error =
          error.response?.data?.detail ||
          error.response?.data?.non_field_errors?.[0] ||
          error.message ||
          "Invalid username or password";
      } finally {
        this.loading = false;
      }
    },

    clearError() {
      this.error = null;
    },
  },
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.login-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  overflow: hidden;
}

.login-header {
  background: linear-gradient(135deg, #004877 0%, #0066cc 100%);
  color: white;
  padding: 40px 30px;
  text-align: center;
}

.login-title {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
}

.logo-text {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  letter-spacing: 0.1em;
}

.login-subtitle {
  margin: 10px 0 0;
  font-size: 1rem;
  opacity: 0.9;
  font-weight: 300;
}

.login-form {
  padding: 40px 30px 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.form-control {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
  background-color: #f8f9fa;
}

.form-control:focus {
  outline: none;
  border-color: #004877;
  background-color: white;
  box-shadow: 0 0 0 3px rgba(0, 72, 119, 0.1);
}

.form-control:disabled {
  background-color: #e9ecef;
  cursor: not-allowed;
}

.form-check {
  display: flex;
  align-items: center;
}

.form-check-input {
  width: 18px;
  height: 18px;
  margin-right: 8px;
  cursor: pointer;
}

.form-check-label {
  cursor: pointer;
  user-select: none;
  color: #666;
  font-size: 14px;
}

.btn {
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-primary {
  background: linear-gradient(135deg, #004877 0%, #0066cc 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 5px 20px rgba(0, 72, 119, 0.3);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.w-100 {
  width: 100%;
}

.alert {
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.alert-danger {
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  color: #721c24;
}

.spinner-border {
  width: 1rem;
  height: 1rem;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spinner-border 0.75s linear infinite;
}

.spinner-border-sm {
  width: 0.875rem;
  height: 0.875rem;
}

@keyframes spinner-border {
  to {
    transform: rotate(360deg);
  }
}

.login-footer {
  padding: 20px 30px;
  background-color: #f8f9fa;
  text-align: center;
  border-top: 1px solid #e9ecef;
}

.text-muted {
  color: #6c757d;
}

.small {
  font-size: 0.875rem;
}

/* Responsive adjustments */
@media (max-width: 480px) {
  .login-header {
    padding: 30px 20px;
  }

  .login-form {
    padding: 30px 20px 20px;
  }

  .login-title {
    font-size: 1.75rem;
  }

  .login-subtitle {
    font-size: 0.9rem;
  }
}

/* Utilities */
.me-1 {
  margin-right: 0.25rem;
}
.me-2 {
  margin-right: 0.5rem;
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
