import Papa, { ParseResult } from "papaparse";

export function parseCSV(file: File, delimiter?: string) {
  return new Promise((resolve, reject) => {
    Papa.parse(file, {
      header: false,
      delimiter: delimiter || ";",
      skipEmptyLines: true,
      complete: (results, file) =>
        resolve({
          result: results,
          file,
        }),
    });
  });
}

export function loadCSV(
  url: string
): Promise<{ result: ParseResult; file?: File }> {
  return new Promise((resolve) => {
    Papa.parse(url, {
      download: true,
      delimiter: ";",
      skipEmptyLines: true,
      complete: (results, file) =>
        resolve({
          result: results,
          file,
        }),
    });
  });
}
