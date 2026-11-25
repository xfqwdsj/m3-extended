config.files.push({
  pattern: `${config.basePath}/kotlin/localization/**/*`,
  included: false,
  watched: false,
});
config.proxies["/localization/"] = "/base/kotlin/localization/";
