import { Recipe } from '@/lib/types/recipe';
import Image from 'next/image';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

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
        <Image
          src={recipe.thumbnail.url}
          alt={recipe.title}
          fill
          className='object-cover'
        />
      </div>
      <CardHeader>
        <h2 className='text-xl font-semibold'>{recipe.title}</h2>
      </CardHeader>
      <CardContent>
        <p className='mb-4 line-clamp-2 text-gray-600'>{recipe.description}</p>
        <div className='mb-3 flex flex-wrap gap-2'>
          {recipe.tags.map((tag) => (
            <Badge
              key={tag.id}
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
