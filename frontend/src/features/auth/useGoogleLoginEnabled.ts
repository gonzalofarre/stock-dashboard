import { useEffect, useState } from "react";
import { authApi } from "./authApi";

/** Whether the backend actually has Google credentials configured — see
 * config/GoogleOAuth2Config.java. Defaults to hidden while loading/on
 * failure so a misconfigured backend never shows a button that 500s. */
export function useGoogleLoginEnabled(): boolean {
  const [enabled, setEnabled] = useState(false);

  useEffect(() => {
    authApi
      .config()
      .then(({ data }) => setEnabled(data.googleLoginEnabled))
      .catch(() => undefined);
  }, []);

  return enabled;
}
