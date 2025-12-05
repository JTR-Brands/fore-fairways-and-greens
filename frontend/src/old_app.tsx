function App() {
  return (
    <div className="min-h-screen bg-fairway-900 text-white">
      <header className="bg-fairway-800 p-4 shadow-lg">
        <h1 className="text-2xl font-bold text-fairway-100">
          FORE: Fairways & Greens
        </h1>
      </header>
      
      <main className="container mx-auto p-8">
        <div className="bg-fairway-800 rounded-lg p-6 shadow-xl">
          <h2 className="text-xl font-semibold mb-4">Welcome to the Course</h2>
          <p className="text-fairway-200">
            Your golf-themed property trading adventure awaits.
          </p>
          
          <div className="mt-6 grid grid-cols-2 gap-4">
            <button className="bg-fairway-600 hover:bg-fairway-500 px-6 py-3 rounded-lg font-medium transition-colors">
              New Game
            </button>
            <button className="bg-sand-300 hover:bg-sand-200 text-fairway-900 px-6 py-3 rounded-lg font-medium transition-colors">
              Join Game
            </button>
          </div>
        </div>
        
        <div className="mt-8 text-center text-fairway-400 text-sm">
          <p>Backend Status: Checking...</p>
        </div>
      </main>
    </div>
  )
}

export default App
