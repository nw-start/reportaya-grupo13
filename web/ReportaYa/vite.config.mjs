import {defineConfig,loadEnv} from 'vite';
export default defineConfig(({mode})=>{
 const env=loadEnv(mode,'.','');
 return {server:{proxy:{'/api':{target:env.API_URL||'https://reportaya-api.onrender.com',changeOrigin:true,timeout:60000,proxyTimeout:60000}}}};
});
