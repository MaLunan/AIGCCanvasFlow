<script setup>
import FlowCanvas from './components/FlowCanvas.vue'
</script>

<template>
  <FlowCanvas />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

#app {
  width: 100%;
  height: 100%;
}
</style>
