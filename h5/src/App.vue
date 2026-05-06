<template>
  <RouterView />
</template>

<style lang="scss">
@use './styles/variables' as *;

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  width: 100vw;
  min-height: 100vh;
  overflow-x: hidden;
  background: $bg-base;
  color: $text-primary;
  font-family: $font-family;
}

#app {
  width: 100%;
  min-height: 100vh;
}
</style>
