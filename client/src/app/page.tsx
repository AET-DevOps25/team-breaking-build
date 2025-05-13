import Image from 'next/image';

export default function Home() {
  return (
    <div className='mx-auto flex h-[75vh] w-full justify-between space-x-2 overflow-hidden py-4'>
      <div className='flex w-1/2 flex-col justify-center'>
        <h1 className='text-5xl font-bold leading-tight'>
          Collaborative recipe making
          <span className='ml-2 text-[#FF7C75]'>redefined</span>
        </h1>
        <p className='mt-6 text-xl text-gray-600'>
          Create, modify, and share your favorite recipes—
          <br />
          and collaborate with others to discover new culinary adventures. <br />
          Track the evolution of each recipe with a detailed history—
          <br />
          all in one intuitive app.
        </p>

        <div className='mt-8 flex space-x-4'>
          <button className='rounded-lg bg-[#FF7C75] px-6 py-3 font-semibold text-white shadow-lg transition duration-300 hover:bg-[#ff4e41]'>
            Get Started
          </button>
          <button className='rounded-lg border-2 border-[#FF7C75] bg-transparent px-6 py-3 font-semibold text-[#FF7C75] shadow-lg transition duration-300 hover:bg-[#FF7C75] hover:text-white'>
            Login
          </button>
        </div>
      </div>
      <div className='relative w-1/2 justify-end overflow-hidden rounded-3xl'>
        <Image
          src='/home-2.jpg'
          alt='Homepage Image'
          width='0'
          height='0'
          sizes='100vw'
          className='h-auto w-full'
        />
      </div>
    </div>
  );
}
