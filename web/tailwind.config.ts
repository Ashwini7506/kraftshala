import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}"
  ],
  theme: {
    extend: {
      colors: {
        kraftshala: { yellow: "#FFC107", deep: "#1A1A2E", accent: "#0F62FE" }
      }
    }
  },
  plugins: []
};
export default config;
