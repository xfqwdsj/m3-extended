config.files.push({
  pattern: `${config.basePath}/kotlin/composeResources/**/*`,
  included: false,
  watched: false,
});
config.proxies["/composeResources/"] = "/base/kotlin/composeResources/";
