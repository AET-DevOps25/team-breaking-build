import { Link } from 'react-router-dom';
import NavLinks from './NavLinks';

export default function NavBar() {
  return (
    <div className='sticky top-0 z-20 shadow-md'>
      <div className='flex justify-between bg-white px-16 py-4'>
        <Link to='/recipes'>
          <img
            src='/logo-color.svg'
            alt='Recipefy Logo'
            width={160}
            height={40}
            className='h-10 w-40'
          />
        </Link>
        <NavLinks />
      </div>
    </div>
  );
}
