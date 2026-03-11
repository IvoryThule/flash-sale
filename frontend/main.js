const { createApp } = Vue;

createApp({
  data() {
    return {
      baseUrl: 'http://localhost:8080/api',
      token: localStorage.getItem('flash_sale_token') || '',
      registerForm: {
        username: '',
        password: '',
        nickname: '',
        phone: ''
      },
      loginForm: {
        username: '',
        password: ''
      },
      result: '等待请求...'
    };
  },
  methods: {
    async register() {
      try {
        const response = await axios.post(`${this.baseUrl}/user/register`, this.registerForm);
        this.result = JSON.stringify(response.data, null, 2);
      } catch (error) {
        this.result = JSON.stringify(error.response?.data || error.message, null, 2);
      }
    },
    async login() {
      try {
        const response = await axios.post(`${this.baseUrl}/user/login`, this.loginForm);
        const token = response.data?.data?.token || '';
        this.token = token;
        if (token) {
          localStorage.setItem('flash_sale_token', token);
        }
        this.result = JSON.stringify(response.data, null, 2);
      } catch (error) {
        this.result = JSON.stringify(error.response?.data || error.message, null, 2);
      }
    },
    async loadUserInfo() {
      try {
        const response = await axios.get(`${this.baseUrl}/user/info`, {
          headers: {
            Authorization: this.token ? `Bearer ${this.token}` : ''
          }
        });
        this.result = JSON.stringify(response.data, null, 2);
      } catch (error) {
        this.result = JSON.stringify(error.response?.data || error.message, null, 2);
      }
    }
  }
}).mount('#app');