import { Link } from "react-router-dom";
import { Tooltip } from "./Tooltip";

/** A ticker that links to its detail page and shows the full company name
 * on hover — see Tooltip for how (and why) the hover bubble works. */
export function TickerLink({ ticker, name, className }: { ticker: string; name?: string | null; className: string }) {
  return (
    <Tooltip text={name}>
      <Link to={`/stock/${ticker}`} className={className}>
        {ticker}
      </Link>
    </Tooltip>
  );
}
