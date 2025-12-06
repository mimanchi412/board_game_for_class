<script setup>
// 路由配置已在main.js中引入，这里不需要额外导入组件
</script>

<template>
  <div id="app">
    <!-- 路由视图出口，用于渲染当前路由对应的组件 -->
    <router-view />
  </div>
</template>

<style>
/* 基础样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #f5f7fa;
  color: #333;
}

#app {
  width: 100%;
  min-height: 100vh;
}
</style>
