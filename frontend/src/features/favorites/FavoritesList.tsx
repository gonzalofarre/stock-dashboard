import { type FormEvent, useEffect, useState } from "react";
import axios from "axios";
import { type Favorite, favoritesApi } from "./favoritesApi";
import "./favorites.css";

/** `refreshSignal`: bump this from a parent whenever a favorite might have
 * changed somewhere else (e.g. the "+ Favorita" button in a suggestion
 * block) so this list re-fetches instead of going stale until next reload. */
export function FavoritesList({ refreshSignal }: { refreshSignal?: number } = {}) {
  const [favorites, setFavorites] = useState<Favorite[] | null>(null);
  const [newTicker, setNewTicker] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  async function load() {
    try {
      const { data } = await favoritesApi.list();
      setFavorites(data);
    } catch {
      setError("No se pudieron cargar tus favoritas.");
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshSignal]);

  async function handleAdd(e: FormEvent) {
    e.preventDefault();
    if (!newTicker.trim()) return;
    setError(null);
    setAdding(true);
    try {
      await favoritesApi.add(newTicker.trim());
      setNewTicker("");
      await load();
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        setError(`${newTicker.toUpperCase()} ya está en tus favoritas.`);
      } else if (axios.isAxiosError(err) && err.response?.status === 400) {
        setError("Ese ticker no parece válido.");
      } else {
        setError("No se pudo agregar. Intentá de nuevo.");
      }
    } finally {
      setAdding(false);
    }
  }

  async function handleRemove(ticker: string) {
    setError(null);
    try {
      await favoritesApi.remove(ticker);
      setFavorites((current) => current?.filter((f) => f.ticker !== ticker) ?? null);
    } catch {
      setError("No se pudo quitar de favoritas.");
    }
  }

  return (
    <div className="favorites-section">
      <h2>Favoritas</h2>
      {error && <div className="favorites-error">{error}</div>}

      <form className="favorites-add-form" onSubmit={handleAdd}>
        <input
          placeholder="Ticker, ej: AAPL"
          value={newTicker}
          onChange={(e) => setNewTicker(e.target.value)}
          maxLength={10}
        />
        <button type="submit" disabled={adding}>
          {adding ? "Agregando..." : "Agregar"}
        </button>
      </form>

      {favorites === null && <p>Cargando...</p>}
      {favorites?.length === 0 && (
        <p className="favorites-empty">Todavía no tenés acciones favoritas. Agregá una arriba.</p>
      )}
      {favorites && favorites.length > 0 && (
        <ul className="favorites-list">
          {favorites.map((favorite) => (
            <li key={favorite.ticker} className="favorite-row">
              <span className="favorite-ticker">{favorite.ticker}</span>
              {favorite.quoteAvailable ? (
                <>
                  <span className="favorite-price">${favorite.price?.toFixed(2)}</span>
                  <span
                    className={`favorite-change ${(favorite.changePercent ?? 0) >= 0 ? "positive" : "negative"}`}
                  >
                    {(favorite.changePercent ?? 0) >= 0 ? "+" : ""}
                    {favorite.changePercent?.toFixed(2)}%
                  </span>
                </>
              ) : (
                <span className="favorite-change unavailable">Sin datos</span>
              )}
              <button
                className="favorite-remove"
                onClick={() => handleRemove(favorite.ticker)}
                aria-label={`Quitar ${favorite.ticker} de favoritas`}
                title="Quitar de favoritas"
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
