/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  transpilePackages: ["@wishpool/design-tokens", "@wishpool/app-fixtures"],
  allowedDevOrigins: ["wishpool.yueying.cloud", "wishpool-admin.yueying.cloud", "wishpool-api.yueying.cloud", "wishpool-admin-api.yueying.cloud", "wishpool-ws.yueying.cloud", "wishpool-media.yueying.cloud", "wishpool-temporal.yueying.cloud"]
};

export default nextConfig;
