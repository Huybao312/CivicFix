import { useEffect, useState, useCallback } from "react";
import { request } from "./api";

export function useRemote<T>(path: string) {
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<{
    key: string;
    data: T | null;
    error: string;
  }>({ key: "", data: null, error: "" });
  const key = path + ":" + revision;
  const reload = useCallback(() => setRevision((n) => n + 1), []);
  useEffect(() => {
    let active = true;
    request<T>(path)
      .then((data) => {
        if (active) setResult({ key, data, error: "" });
      })
      .catch((e) => {
        if (active) setResult({ key, data: null, error: e.message });
      });
    return () => {
      active = false;
    };
  }, [path, key]);
  return {
    data: result.key === key ? result.data : null,
    error: result.key === key ? result.error : "",
    busy: result.key !== key,
    reload,
  };
}
