/**
 * Tokens live in localStorage for now — simplest option to ship Phase 1.
 * Tradeoff worth knowing: unlike an httpOnly cookie, this is readable by any
 * JS on the page, so it's vulnerable if the app ever has an XSS bug. Revisit
 * with httpOnly cookies (set by the backend) before this app handles
 * anything more sensitive than favorite tickers.
 */
const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";

export const authStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};
