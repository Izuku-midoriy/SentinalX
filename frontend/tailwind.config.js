/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        soc: {
          bg: '#090d16',
          card: '#0f172a',
          border: '#1e293b',
          muted: '#64748b',
          accent: '#3b82f6',
          critical: '#ef4444',
          high: '#f97316',
          medium: '#eab308',
          low: '#3b82f6',
          info: '#10b981',
        }
      }
    },
  },
  plugins: [],
}
