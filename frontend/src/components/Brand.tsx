import "./brand.css";

export function Brand({ size = "md" }: { size?: "sm" | "md" }) {
  return (
    <div className={`brand brand-${size}`}>
      <img src="/stockdash-icon.png" alt="" className="brand-icon" />
      <span className="brand-name">Stock Dash</span>
    </div>
  );
}
