/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      screens: {
        side: '860px',
      },
      colors: {
        cream: {
          50: '#fdfbf7',
          100: '#f7f4ef',
          200: '#f3efe8',
          300: '#efe8dd',
          400: '#e7e0d5',
          500: '#ddd2c2',
        },
        ink: {
          900: '#241b15',
          800: '#2b211a',
          700: '#4a3b2e',
          600: '#5d5044',
          500: '#6f6154',
          400: '#8d7f70',
          300: '#9a8b7c',
          100: '#f3ebe1',
        },
        terra: {
          700: '#96401d',
          600: '#b85327',
          500: '#c2592c',
          100: '#fdf3ee',
        },
        ok: {
          700: '#2f7a5f',
          600: '#3f8f6f',
          50: '#e6f4ef',
          100: '#e3f1ec',
        },
        info: {
          700: '#4d6f9e',
          100: '#e7eef8',
        },
        danger: {
          700: '#c93f2b',
          600: '#d64a36',
          100: '#fdecea',
        },
        warn: {
          700: '#b07b2f',
          100: '#f6ead9',
        },
        surface: {
          dark: '#1c1714',
          sidebar: '#161210',
        },
      },
      fontFamily: {
        sans: [
          'Inter',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Roboto',
          'sans-serif',
        ],
        display: ['Lora', 'Georgia', 'serif'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(58, 36, 20, 0.04), 0 10px 24px rgba(58, 36, 20, 0.05)',
        pill: '0 1px 2px rgba(58, 36, 20, 0.05)',
      },
    },
  },
  plugins: [],
};
