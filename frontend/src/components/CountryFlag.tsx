interface CountryFlagProps {
  iso: string | null | undefined;
}

export function CountryFlag({ iso }: CountryFlagProps) {
  if (!iso || iso.length !== 2) {
    return <span className="text-slate-500">--</span>;
  }

  const flag = iso
    .toUpperCase()
    .split('')
    .map((letter) => String.fromCodePoint(127397 + letter.charCodeAt(0)))
    .join('');

  return (
    <span aria-label={iso.toUpperCase()} className="text-base">
      {flag}
    </span>
  );
}
