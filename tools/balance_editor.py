"""Metrum balance editor.

Sliders (with min/max limits) for every value in assets/balance.properties.
"Zapisz" saves the file, "Eksportuj do gry" saves + runs `gradlew deploy`
(builds the jar with the values baked in and installs it into saves/mods/Metrum.zip).

Run:  python tools/balance_editor.py
"""
import pathlib
import re
import subprocess
import threading
import tkinter as tk
from tkinter import ttk, messagebox

ROOT = pathlib.Path(__file__).resolve().parent.parent
FILE = ROOT / "assets" / "balance.properties"
LINE = re.compile(r"^\s*([\w.]+)\s*=\s*([-\d.]+)\s*#\s*([-\d.]+)\s*\|\s*([-\d.]+)\s*\|\s*(.*)$")


def load():
    rows, raw = [], FILE.read_text(encoding="utf-8").splitlines()
    for i, line in enumerate(raw):
        m = LINE.match(line)
        if m:
            key, val, lo, hi, label = m.groups()
            rows.append(dict(i=i, key=key, val=float(val), lo=float(lo), hi=float(hi), label=label.strip(),
                             integer=all(float(x).is_integer() for x in (val, lo, hi))))
    return raw, rows


def fmt(v, integer):
    return str(int(round(v))) if integer else f"{v:.3f}".rstrip("0").rstrip(".")


class Editor(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Metrum – edytor balansu")
        self.geometry("760x720")
        self.raw, self.rows = load()
        self.vars = {}

        frame = ttk.Frame(self, padding=8)
        frame.pack(fill="both", expand=True)
        canvas = tk.Canvas(frame, highlightthickness=0)
        scroll = ttk.Scrollbar(frame, orient="vertical", command=canvas.yview)
        inner = ttk.Frame(canvas)
        inner.bind("<Configure>", lambda e: canvas.configure(scrollregion=canvas.bbox("all")))
        canvas.create_window((0, 0), window=inner, anchor="nw")
        canvas.configure(yscrollcommand=scroll.set)
        canvas.pack(side="left", fill="both", expand=True)
        scroll.pack(side="right", fill="y")

        for r, row in enumerate(self.rows):
            ttk.Label(inner, text=row["label"], width=34).grid(row=r, column=0, sticky="w", pady=2)
            var = tk.DoubleVar(value=row["val"])
            self.vars[row["key"]] = var
            entry = ttk.Entry(inner, width=9)
            entry.insert(0, fmt(row["val"], row["integer"]))
            scale = ttk.Scale(inner, from_=row["lo"], to=row["hi"], variable=var, length=280)
            scale.grid(row=r, column=1, padx=6)
            entry.grid(row=r, column=2)
            ttk.Label(inner, text=f"[{fmt(row['lo'], row['integer'])}–{fmt(row['hi'], row['integer'])}]",
                      foreground="#777").grid(row=r, column=3, padx=4)

            def on_scale(*_, var=var, entry=entry, row=row):
                entry.delete(0, "end")
                entry.insert(0, fmt(var.get(), row["integer"]))

            def on_entry(_e, var=var, entry=entry, row=row):
                try:
                    v = min(max(float(entry.get().replace(",", ".")), row["lo"]), row["hi"])  # enforce limits
                except ValueError:
                    v = var.get()
                var.set(v)
                on_scale()

            var.trace_add("write", on_scale)
            entry.bind("<Return>", on_entry)
            entry.bind("<FocusOut>", on_entry)

        bar = ttk.Frame(self, padding=8)
        bar.pack(fill="x")
        ttk.Button(bar, text="Przywróć z pliku", command=self.reload).pack(side="left")
        ttk.Button(bar, text="Zapisz", command=self.save).pack(side="left", padx=6)
        ttk.Button(bar, text="Eksportuj do gry (jar)", command=self.export).pack(side="left")
        self.status = ttk.Label(bar, text="")
        self.status.pack(side="left", padx=10)

    def reload(self):
        self.raw, rows = load()
        for row in rows:
            self.vars[row["key"]].set(row["val"])
        self.status.config(text="Wczytano z pliku")

    def save(self):
        for row in self.rows:
            v = fmt(self.vars[row["key"]].get(), row["integer"])
            line = self.raw[row["i"]]
            self.raw[row["i"]] = re.sub(r"=\s*[-\d.]+", f"= {v}", line, count=1)
        FILE.write_text("\n".join(self.raw) + "\n", encoding="utf-8")
        self.status.config(text="Zapisano balance.properties")

    def export(self):
        self.save()
        self.status.config(text="Budowanie i instalacja…")
        gradlew = ROOT / ("gradlew.bat" if (ROOT / "gradlew.bat").exists() else "gradlew")

        def run():
            p = subprocess.run([str(gradlew), "deploy", "-q"], cwd=ROOT, capture_output=True, text=True, shell=True)
            ok = p.returncode == 0
            msg = "Gotowe: Metrum.zip w saves/mods (zrestartuj grę)" if ok else "Błąd budowania"
            self.after(0, lambda: self.status.config(text=msg))
            if not ok:
                self.after(0, lambda: messagebox.showerror("Błąd", (p.stdout + p.stderr)[-2000:]))

        threading.Thread(target=run, daemon=True).start()


if __name__ == "__main__":
    Editor().mainloop()
