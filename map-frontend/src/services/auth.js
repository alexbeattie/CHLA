import axios from 'axios';

const API_BASE = (import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000").replace(
  /\/+$/,
  ""
);

class AuthService {
  constructor() {
    this.token = null;
    this.user = null;
    this.tokenKey = 'chla_auth_token';
    this.userKey = 'chla_auth_user';
    this.rememberKey = 'chla_remember_me';
    
    // Load token from storage on initialization
    this.loadFromStorage();
    
    // Set up axios interceptor for auth headers
    this.setupAxiosInterceptor();
  }

  loadFromStorage() {
    // Check if user chose to be remembered
    const rememberMe = localStorage.getItem(this.rememberKey) === 'true';
    const storage = rememberMe ? localStorage : sessionStorage;
    
    const storedToken = storage.getItem(this.tokenKey);
    const storedUser = storage.getItem(this.userKey);
    
    if (storedToken) {
      this.token = storedToken;
    }
    
    if (storedUser) {
      try {
        this.user = JSON.parse(storedUser);
      } catch (e) {
        console.error('Error parsing stored user:', e);
      }
    }
  }

  setupAxiosInterceptor() {
    // Request interceptor to add auth header
    axios.interceptors.request.use(
      (config) => {
        if (this.token) {
          config.headers.Authorization = `Token ${this.token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor to handle 401 errors
    axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Token is invalid or expired
          this.logout();
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  async login(username, password, rememberMe = false) {
    try {
      // Call Django REST Framework token endpoint
      const response = await axios.post(`${API_BASE}/api/users/auth/login/`, {
        username,
        password
      });

      const { token, user } = response.data;
      
      // Store token and user info
      this.token = token;
      this.user = user;
      
      // Choose storage based on remember me
      const storage = rememberMe ? localStorage : sessionStorage;
      
      // Save to storage
      storage.setItem(this.tokenKey, token);
      storage.setItem(this.userKey, JSON.stringify(user));
      localStorage.setItem(this.rememberKey, rememberMe.toString());
      
      return response.data;
    } catch (error) {
      throw error;
    }
  }

  logout() {
    // Clear token and user
    this.token = null;
    this.user = null;
    
    // Clear from both storages
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    sessionStorage.removeItem(this.tokenKey);
    sessionStorage.removeItem(this.userKey);
  }

  isAuthenticated() {
    return !!this.token;
  }

  getToken() {
    return this.token;
  }

  getUser() {
    return this.user;
  }

  hasPermission(permission) {
    if (!this.user || !this.user.permissions) {
      return false;
    }
    return this.user.permissions.includes(permission);
  }

  isStaff() {
    return this.user?.is_staff || false;
  }

  isSuperuser() {
    return this.user?.is_superuser || false;
  }
}

// Create singleton instance
export const authService = new AuthService();
