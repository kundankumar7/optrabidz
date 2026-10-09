# Typeface sources and maintenance

OptraBidz uses two Fontshare Originals under the ITF Free Font License:

- Cabinet Grotesk for display and heading roles. Approved weights: 500; 700 only for deliberate display emphasis.
- General Sans for product copy, labels, and numeric data. Approved weights: 400, 500, and 600.

No font binaries are stored in this repository. Obtain the fonts only from their official Fontshare family pages or load them through an approved Fontshare delivery method. Before any production integration, verify the current family license and delivery terms against the authoritative Fontshare license shown on the family page.

Do not modify, subset, convert, rename, or change font metadata. Do not commit downloaded font files or pass them to another person or service. These restrictions apply even when the intended product use is permitted.

The design-token fallbacks are part of the contract:

- Cabinet Grotesk, Arial, sans-serif
- General Sans, Arial, sans-serif

Any future font-loading implementation must test the primary and fallback stacks at the supported viewport widths. Text must remain readable, controls must not clip, numeric tables must remain aligned, and layout shift must stay within the product performance budget.

License provenance is recorded in `../licenses/Fontshare-ITF-FFL.txt`.
