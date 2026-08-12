/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  transpilePackages: ["@wishpool/design-tokens", "@wishpool/app-fixtures"]
};

export default nextConfig;
