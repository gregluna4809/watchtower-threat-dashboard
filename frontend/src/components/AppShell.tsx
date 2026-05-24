import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { Cpu, Gauge, Globe2, Network, ShieldCheck, SlidersHorizontal } from 'lucide-react';
import { HealthPill } from './HealthPill';
import { cn } from '../lib/utils';

const navItems = [
  { to: '/', label: 'Dashboard', icon: Gauge },
  { to: '/connections', label: 'Connections', icon: Network },
  { to: '/processes', label: 'Processes', icon: Cpu },
  { to: '/endpoints', label: 'Endpoints', icon: Globe2 },
  { to: '/rules', label: 'Rules', icon: SlidersHorizontal }
];

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-slate-800 bg-slate-950 lg:block">
        <div className="flex h-20 items-center gap-3 px-6">
          <div className="flex h-10 w-10 items-center justify-center rounded-md border border-amber-500/40 bg-amber-500/10">
            <ShieldCheck className="h-5 w-5 text-amber-500" />
          </div>
          <div>
            <div className="text-sm font-semibold uppercase text-slate-100">Watchtower</div>
            <div className="text-xs text-slate-500">Local advisory console</div>
          </div>
        </div>
        <nav className="space-y-1 px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:bg-slate-900 hover:text-slate-100'
                )
              }
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 border-b border-slate-800 bg-slate-950/95 backdrop-blur">
          <div className="flex h-20 items-center justify-between gap-4 px-8">
            <div>
              <div className="text-xs font-medium uppercase text-amber-500">Watchtower</div>
              <h1 className="text-xl font-semibold text-slate-100">Network Threat Dashboard</h1>
            </div>
            <HealthPill />
          </div>
          <nav className="flex gap-2 overflow-x-auto border-t border-slate-800 px-8 py-3 lg:hidden">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  cn(
                    'inline-flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium',
                    isActive ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:bg-slate-900'
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
          </nav>
        </header>

        <main className="p-8">{children}</main>
      </div>
    </div>
  );
}
