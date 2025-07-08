import { Recipe } from '@/lib/types/recipe';
import Image from 'next/image';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ImageIcon } from 'lucide-react';

interface RecipeCardProps {
  recipe: Recipe;
  onClick?: () => void;
}

export function RecipeCard({ recipe, onClick }: RecipeCardProps) {
  return (
    <Card
      className='cursor-pointer overflow-hidden transition-shadow hover:shadow-lg'
      onClick={onClick}
    >
      <div className='relative h-48'>
        {recipe.thumbnail?.base64String ? (
          <Image
            src={`data:image/jpeg;base64,${recipe.thumbnail.base64String}`}
            alt={recipe.title}
            fill
            className='object-cover'
          />
        ) : (
          <div className='flex h-full flex-col items-center justify-center bg-gray-100'>
            <ImageIcon className='mb-2 size-12 text-gray-400' />
            <p className='text-center text-sm font-medium text-gray-500'>
              This recipe is camera shy!
              <br />
              <span className='text-xs'>But trust us, it tastes amazing 😋</span>
            </p>
          </div>
        )}
      </div>
      <CardHeader>
        <h2 className='text-xl font-semibold'>{recipe.title}</h2>
      </CardHeader>
      <CardContent>
        <p className='mb-4 line-clamp-2 text-gray-600'>{recipe.description}</p>
        <div className='mb-3 flex flex-wrap gap-2'>
          {recipe.tags.map((tag) => (
            <Badge
              key={tag.id || tag.name}
              variant='secondary'
            >
              {tag.name}
            </Badge>
          ))}
        </div>
        <div className='text-sm text-gray-500'>
          <span>{recipe.servingSize} servings</span>
        </div>
      </CardContent>
    </Card>
  );
}
