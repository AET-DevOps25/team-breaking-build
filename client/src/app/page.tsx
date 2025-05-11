import Image from "next/image";

export default function Home() {
  return (
        <div className="p-12 flex justify-between w-full mx-auto max-w-11xl h-[85vh] overflow-hidden">
            <div className="w-1/2 flex flex-col justify-center">
                <h1 className="text-5xl font-bold leading-tight">
                    Collaborative recipe making
                    <span className="text-[#FF7C75] ml-2">redefined</span>
                </h1>
                <p className="text-xl mt-6 text-gray-600">
                    Create, modify, and share your favorite recipes—<br/>
                    and collaborate with others to discover new culinary adventures. <br/>
                    Track the evolution of each recipe with a detailed history—<br/>
                    all in one intuitive app.
                </p>

                <div className="mt-8 flex space-x-4">
                    <button
                        className="px-6 py-3 bg-[#FF7C75] text-white font-semibold rounded-lg shadow-lg hover:bg-[#ff4e41] transition duration-300">
                        Get Started
                    </button>
                    <button
                        className="px-6 py-3 bg-transparent border-2 border-[#FF7C75] text-[#FF7C75] font-semibold rounded-lg shadow-lg hover:bg-[#FF7C75] hover:text-white transition duration-300">
                        Login
                    </button>
                </div>
            </div>
            <div className="w-1/2 relative slash-divider justify-end overflow-hidden rounded-4xl">
                <Image
                    src='/home-2.jpg'
                    alt='Homepage Image'
                    width="0"
                    height="0"
                    sizes="100vw"
                    className="w-full h-auto"
                />
            </div>
        </div>
  );
}
