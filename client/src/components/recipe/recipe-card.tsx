import { Recipe } from '@/lib/types/recipe';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ImageIcon, Users, Utensils, FileText, Tags } from 'lucide-react';
import { UserInfo } from '@/components/user';

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
          <img
            src={`data:image/jpeg;base64,${recipe.thumbnail.base64String}`}
            alt={recipe.title}
            className='h-full w-full object-cover'
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
        <h2 className='flex items-center text-xl font-semibold'>
          <Utensils className='mr-2 size-5 text-[#FF7C75]' />
          {recipe.title}
        </h2>
      </CardHeader>
      <CardContent>
        <p className='mb-4 line-clamp-2 flex items-start text-gray-600'>
          <FileText className='mr-2 mt-0.5 size-4 text-gray-400' />
          {recipe.description}
        </p>
        <div className='mb-3 flex flex-wrap gap-2'>
          <div className='flex items-center'>
            <Tags className='mr-1 size-4 text-gray-400' />
          </div>
          {recipe.tags.map((tag) => (
            <Badge
              key={tag.id || tag.name}
              variant='secondary'
            >
              {tag.name}
            </Badge>
          ))}
        </div>
        <div className='flex items-center justify-between text-sm text-gray-500'>
          <div className='flex items-center'>
            <Users className='mr-1 size-4' />
            <span>{recipe.servingSize} servings</span>
          </div>
          {recipe.userId && (
            <UserInfo
              userId={recipe.userId}
              size='sm'
              clickable={false}
            />
          )}
        </div>
      </CardContent>
    </Card>
  );
}
