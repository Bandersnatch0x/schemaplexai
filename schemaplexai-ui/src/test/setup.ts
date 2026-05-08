import '@testing-library/jest-dom'

// Polyfill window.matchMedia for Ant Design responsive observer (jsdom)
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

// Polyfill DataTransfer for drag-and-drop / file input tests in jsdom
class DataTransferPolyfill {
  items: { add: (file: File) => void; files: File[] } = {
    files: [],
    add(file: File) {
      this.files.push(file)
    },
  }
  get files(): FileList {
    const files = this.items.files
    return {
      length: files.length,
      item: (index: number) => files[index] ?? null,
      [Symbol.iterator]: function* () {
        for (let i = 0; i < files.length; i++) {
          yield files[i]
        }
      },
    } as unknown as FileList
  }
}

Object.defineProperty(globalThis, 'DataTransfer', {
  value: DataTransferPolyfill,
  writable: true,
  configurable: true,
})
