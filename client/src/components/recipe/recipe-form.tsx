import { useState, useRef, useEffect, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { X, Image as ImageIcon, Plus, Trash2 } from 'lucide-react';
import { Tag } from '@/lib/services/recipeService';
import { Textarea } from '@/components/ui/textarea';

// Utility function to format tag names from ALL_CAPS_WITH_UNDERSCORES to Capitalized
function formatTagName(tagName: string): string {
  return tagName
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

const recipeSchema = z.object({
  title: z.string().min(1, 'Title is required'),
  description: z.string().min(1, 'Description is required'),
  servingSize: z.number().min(1, 'Serving size must be at least 1'),
  tags: z.array(z.string()).min(1, 'At least one tag is required'),
  thumbnail: z.instanceof(File).optional(),
  ingredients: z
    .array(
      z.object({
        name: z.string().min(1, 'Ingredient name is required'),
        amount: z.number().min(0, 'Amount must be positive'),
        unit: z.string().min(1, 'Unit is required'),
      }),
    )
    .min(1, 'At least one ingredient is required'),
  steps: z
    .array(
      z.object({
        order: z.number(),
        details: z.string().min(1, 'Step details are required'),
        image: z.instanceof(File).optional(),
      }),
    )
    .min(1, 'At least one step is required'),
});

type RecipeFormData = z.infer<typeof recipeSchema>;

interface RecipeFormProps {
  onSubmit: (data: RecipeFormData) => Promise<void>;
  isSubmitting?: boolean;
  availableTags?: Tag[];
  isLoadingTags?: boolean;
  selectedTags?: Tag[];
  setSelectedTags?: (tags: Tag[]) => void;
  prefillData?: Partial<RecipeFormData>;
  onChange?: (data: RecipeFormData) => void;
  submitButtonText?: string;
  disabled?: boolean;
  commitMessage?: string;
  onCommitMessageChange?: (message: string) => void;
  showCommitMessage?: boolean;
  existingImages?: {
    thumbnail?: string;
    stepImages?: (string | null)[];
  };
}

const ingredientPlaceholders = [
  'A pinch of magic',
  "Grandma's secret ingredient",
  'Something that makes it pop',
  'The star of the show',
  'That one thing you always forget',
  'The ingredient that makes everyone ask for seconds',
  "Your kitchen's MVP",
  'The unsung hero of this dish',
  'The ingredient that brings it all together',
  'That special something',
  'The ingredient that makes it Instagram-worthy',
  'The secret weapon',
  'The ingredient that makes your taste buds dance',
  'The one that makes it unique',
  'The ingredient that makes it a family favorite',
  'The ingredient that makes it restaurant-worthy',
  'The ingredient that makes it a crowd-pleaser',
  'The ingredient that makes it a conversation starter',
  'The ingredient that makes it a comfort food',
  'The ingredient that makes it a gourmet dish',
  'The ingredient that makes it a healthy choice',
  'The ingredient that makes it a guilty pleasure',
  'The ingredient that makes it a party favorite',
  'The ingredient that makes it a date night special',
  'The ingredient that makes it a brunch star',
] as const;

const stepPlaceholders = [
  'First, take a deep breath and channel your inner chef',
  "Start by making sure your kitchen doesn't look like a tornado hit it",
  'Grab your favorite cooking playlist and press play',
  "Put on your imaginary chef's hat (or a real one if you have it)",
  'Channel your inner Gordon Ramsay (but maybe be a bit nicer)',
  "Pretend you're on a cooking show and explain what you're doing",
  "Make sure your cat isn't planning to steal your ingredients",
  "Double-check that you didn't forget to preheat the oven",
  'Take a moment to appreciate how good your kitchen smells',
  'Do a little happy dance because cooking is fun',
  'Make sure your phone is charged for taking food pics',
  'Check if your neighbors are home (they might want to taste test)',
  "Take a selfie with your creation (it's mandatory)",
  'Make sure your food styling is on point',
  'Channel your inner food blogger and get creative',
  "Pretend you're in a cooking competition",
  'Make sure your plating is Instagram-worthy',
  'Take a moment to appreciate your culinary skills',
  'Do a taste test (quality control is important)',
  'Make sure your food looks as good as it tastes',
  'Channel your inner food critic and be impressed',
  'Take a moment to appreciate the art of cooking',
  'Make sure your food tells a story',
  "Pretend you're writing a cookbook",
  'Take a moment to be proud of your creation',
] as const;

export function RecipeForm({
  onSubmit,
  isSubmitting,
  availableTags = [],
  isLoadingTags = false,
  selectedTags: externalSelectedTags,
  setSelectedTags: externalSetSelectedTags,
  prefillData,
  onChange,
  submitButtonText = 'Save Recipe',
  disabled = false,
  commitMessage = '',
  onCommitMessageChange,
  showCommitMessage = false,
  existingImages,
}: RecipeFormProps) {
  const [internalSelectedTags, setInternalSelectedTags] = useState<Tag[]>([]);
  const [internalAvailableTags, setInternalAvailableTags] = useState<Tag[]>([]);
  const [internalIsLoadingTags, setInternalIsLoadingTags] = useState(false);

  // Use external props if provided, otherwise use internal state
  const selectedTags = externalSelectedTags || internalSelectedTags;
  const setSelectedTags = externalSetSelectedTags || setInternalSelectedTags;
  const availableTagsToUse = availableTags || internalAvailableTags;
  const isLoadingTagsToUse = isLoadingTags || internalIsLoadingTags;
  const [searchQuery, setSearchQuery] = useState('');
  const [showTagDropdown, setShowTagDropdown] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(existingImages?.thumbnail || null);
  const [stepImages, setStepImages] = useState<(string | null)[]>(existingImages?.stepImages || []);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const stepImageRefs = useRef<(HTMLInputElement | null)[]>([]);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Load tags if using internal state
  useEffect(() => {
    if (!availableTags && !isLoadingTags) {
      const fetchTags = async () => {
        try {
          setInternalIsLoadingTags(true);
          const { getTags } = await import('@/lib/services/recipeService');
          const tags = await getTags();
          setInternalAvailableTags(tags);
        } catch (error) {
          console.error('Failed to load tags:', error);
        } finally {
          setInternalIsLoadingTags(false);
        }
      };
      fetchTags();
    }
  }, [availableTags, isLoadingTags]);

  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
    watch,
    reset,
  } = useForm<RecipeFormData>({
    resolver: zodResolver(recipeSchema),
    defaultValues: prefillData
      ? {
        ...prefillData,
        tags: prefillData.tags || [],
        ingredients: prefillData.ingredients || [{ name: '', amount: 0, unit: '' }],
        steps: prefillData.steps || [{ order: 1, details: '', image: undefined }],
      }
      : {
        tags: [],
        ingredients: [{ name: '', amount: 0, unit: '' }],
        steps: [{ order: 1, details: '', image: undefined }],
      },
  });

  const ingredients = watch('ingredients') || [];
  const steps = watch('steps') || [];

  // Call onChange when form data changes
  useEffect(() => {
    if (onChange) {
      const subscription = watch((value) => {
        onChange(value as RecipeFormData);
      });
      return () => subscription.unsubscribe();
    }
  }, [watch, onChange]);

  // Initialize persistent placeholders
  const { ingredientPlaceholders: shuffledIngredients, stepPlaceholders: shuffledSteps } = useMemo(() => {
    const shuffledIngredients = [...ingredientPlaceholders].sort(() => Math.random() - 0.5);
    const shuffledSteps = [...stepPlaceholders].sort(() => Math.random() - 0.5);
    return {
      ingredientPlaceholders: shuffledIngredients,
      stepPlaceholders: shuffledSteps,
    };
  }, []); // Empty dependency array means this only runs once on mount

  const getPlaceholder = (placeholders: readonly string[], index: number): string => {
    return placeholders[index % placeholders.length];
  };

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowTagDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const filteredTags = availableTagsToUse.filter((tag) => tag.name.toLowerCase().includes(searchQuery.toLowerCase()));

  const handleTagSelect = (tag: Tag) => {
    if (!selectedTags) return;
    if (!selectedTags.some((t) => t.id === tag.id)) {
      const newTags = [...selectedTags, tag];
      setSelectedTags(newTags);
      setValue(
        'tags',
        newTags.map((t) => t.name),
      );
    }
    setSearchQuery('');
    setShowTagDropdown(false);
  };

  const handleTagRemove = (tagToRemove: string) => {
    if (!selectedTags) return;
    const newTags = selectedTags.filter((tag) => tag.name !== tagToRemove);
    setSelectedTags(newTags);
    setValue(
      'tags',
      newTags.map((t) => t.name),
    );
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setValue('thumbnail', file);
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);
    }
  };

  const handleImageClick = () => {
    if (previewUrl) {
      fileInputRef.current?.click();
    } else {
      fileInputRef.current?.click();
    }
  };

  const handleStepImageChange = (index: number) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setValue(`steps.${index}.image`, file);
      const url = URL.createObjectURL(file);
      setStepImages((prev) => {
        const newImages = [...prev];
        newImages[index] = url;
        return newImages;
      });
    }
  };

  const handleStepImageClick = (index: number) => {
    stepImageRefs.current[index]?.click();
  };

  const handleStepImageRemove = (index: number) => (e: React.MouseEvent) => {
    e.stopPropagation();
    setValue(`steps.${index}.image`, undefined);
    setStepImages((prev) => {
      const newImages = [...prev];
      newImages[index] = null;
      return newImages;
    });
  };

  const addIngredient = () => {
    setValue('ingredients', [...ingredients, { name: '', amount: 0, unit: '' }]);
  };

  const removeIngredient = (index: number) => {
    if (ingredients.length > 1) {
      setValue(
        'ingredients',
        ingredients.filter((_, i) => i !== index),
      );
    }
  };

  const addStep = () => {
    setValue('steps', [...steps, { order: steps.length + 1, details: '', image: undefined }]);
    setStepImages((prev) => [...prev, null]);
  };

  const removeStep = (index: number) => {
    if (steps.length > 1) {
      const newSteps = steps
        .filter((_, i) => i !== index)
        .map((step, i) => ({
          ...step,
          order: i + 1,
        }));
      setValue('steps', newSteps);
      setStepImages((prev) => prev.filter((_, i) => i !== index));
    }
  };

  // Add this effect to update the form when prefillData changes
  useEffect(() => {
    if (prefillData) {
      reset({
        ...prefillData,
        tags: prefillData.tags || [],
        ingredients: prefillData.ingredients || [{ name: '', amount: 0, unit: '' }],
        steps: prefillData.steps || [{ order: 1, details: '', image: undefined }],
      });
    }
  }, [prefillData, reset]);

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className='space-y-6'
    >
      <div className='space-y-6'>
        <div className='space-y-2'>
          <Label htmlFor='thumbnail'>Thumbnail</Label>
          <div
            onClick={handleImageClick}
            className='relative cursor-pointer rounded-lg border-2 border-dashed border-[#FF7C75] p-4 transition-colors hover:border-[#FF7C75]/80'
          >
            <input
              type='file'
              ref={fileInputRef}
              onChange={handleImageChange}
              accept='image/*'
              className='hidden'
            />
            {previewUrl ? (
              <div className='relative'>
                <img
                  src={previewUrl}
                  alt='Preview'
                  className='h-48 w-full rounded-lg object-cover'
                />
                <Button
                  type='button'
                  variant='secondary'
                  className='absolute bottom-2 right-2 bg-white/90 hover:bg-white'
                  onClick={(e) => {
                    e.stopPropagation();
                    fileInputRef.current?.click();
                  }}
                >
                  Change Image
                </Button>
              </div>
            ) : (
              <div className='flex h-48 flex-col items-center justify-center text-gray-500'>
                <ImageIcon className='mb-2 size-12' />
                <p className='text-sm font-medium'>Click to upload thumbnail</p>
                <p className='text-xs text-gray-400'>PNG, JPG up to 5MB</p>
              </div>
            )}
          </div>
          {errors.thumbnail && <p className='text-sm text-red-500'>{errors.thumbnail.message}</p>}
        </div>

        <div className='space-y-2'>
          <Label htmlFor='title'>Title</Label>
          <Textarea
            id='title'
            {...register('title')}
            placeholder='Give your recipe a mouthwatering name...'
            className='min-h-[60px] border-[#FF7C75] focus:border-[#FF7C75] focus:ring-[#FF7C75]'
          />
          {errors.title && <p className='text-sm text-red-500'>{errors.title.message}</p>}
        </div>

        <div className='space-y-2'>
          <Label htmlFor='description'>Description</Label>
          <Textarea
            id='description'
            {...register('description')}
            placeholder="Tell us why this recipe is special... What makes it unique? What's the story behind it? Share your culinary inspiration!"
            className='min-h-[120px] border-[#FF7C75] focus:border-[#FF7C75] focus:ring-[#FF7C75]'
          />
          {errors.description && <p className='text-sm text-red-500'>{errors.description.message}</p>}
        </div>

        <div className='space-y-2'>
          <Label htmlFor='servingSize'>Serving Size</Label>
          <Input
            id='servingSize'
            type='number'
            min={1}
            placeholder='How many hungry souls will this feed?'
            {...register('servingSize', { valueAsNumber: true })}
            className='border-[#FF7C75] focus:border-[#FF7C75] focus:ring-[#FF7C75]'
          />
          {errors.servingSize && <p className='text-sm text-red-500'>{errors.servingSize.message}</p>}
        </div>

        <div className='space-y-2'>
          <Label htmlFor='tags'>Tags</Label>
          <div
            className='relative'
            ref={dropdownRef}
          >
            <Input
              id='tags'
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setShowTagDropdown(true);
              }}
              onFocus={() => setShowTagDropdown(true)}
              placeholder='Help others find your recipe...'
              className='border-[#FF7C75] focus:border-[#FF7C75] focus:ring-[#FF7C75]'
            />
            {showTagDropdown && (
              <div className='absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md border border-gray-200 bg-white shadow-lg'>
                {isLoadingTagsToUse ? (
                  <div className='p-2 text-center text-gray-500'>Loading tags...</div>
                ) : filteredTags.length === 0 ? (
                  <div className='p-2 text-center text-gray-500'>No tags found</div>
                ) : (
                  filteredTags.map((tag) => (
                    <div
                      key={tag.id}
                      className='cursor-pointer px-4 py-2 hover:bg-gray-100'
                      onClick={() => handleTagSelect(tag)}
                    >
                      {formatTagName(tag.name)}
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
          <div className='mt-2 flex flex-wrap gap-2'>
            {selectedTags?.map((tag) => (
              <div
                key={tag.id}
                className='flex items-center gap-1 rounded-full bg-[#FF7C75] px-3 py-1 text-sm text-white'
              >
                {formatTagName(tag.name)}
                <button
                  type='button'
                  onClick={() => handleTagRemove(tag.name)}
                  className='hover:text-gray-200'
                >
                  <X className='size-4' />
                </button>
              </div>
            ))}
          </div>
          {errors.tags && <p className='text-sm text-red-500'>{errors.tags.message}</p>}
        </div>
      </div>

      <div className='relative py-8'>
        <div
          className='absolute inset-0 flex items-center'
          aria-hidden='true'
        >
          <div className='w-full border-t border-gray-200'></div>
        </div>
        <div className='relative flex justify-center'>
          <span className='bg-white px-6 text-sm font-medium text-gray-500'>Let&apos;s Get Cooking!</span>
        </div>
      </div>

      <div className='space-y-4'>
        <div className='flex items-center justify-between'>
          <Label>Ingredients</Label>
          <Button
            type='button'
            variant='outline'
            size='sm'
            onClick={addIngredient}
            className='border-[#FF7C75] text-[#FF7C75] hover:bg-[#FF7C75] hover:text-white'
          >
            <Plus className='mr-2 size-4' />
            Add Ingredient
          </Button>
        </div>
        {ingredients?.map((_, index) => (
          <div
            key={index}
            className='flex items-start gap-4'
          >
            <div className='flex-1'>
              <Input
                {...register(`ingredients.${index}.name`)}
                placeholder={getPlaceholder(shuffledIngredients, index)}
                className='border-[#FF7C75] focus:border-[#FF7C75] focus:ring-[#FF7C75]'
              />
            </div>
            <div className='w-24'>
              <Input
                type='number'
                step='0.01'
                min='0'
                {...register(`ingredients.${index}.amount`, { valueAsNumber: true })}
                placeholder='How much?'
                className='border-[#FF7C75] focus:border-[#FF7C75] focus:ring-[#FF7C75]'
              />
              <p className='mt-1 text-xs text-gray-500'>Quantity</p>
            </div>
            <div className='w-24'>
              <Input
                {...register(`ingredients.${index}.unit`)}
                placeholder='Unit'
                className='border-[#FF7C75] focus:border-[#FF7C75] focus:ring-[#FF7C75]'
              />
            </div>
            {ingredients.length > 1 && (
              <Button
                type='button'
                variant='ghost'
                size='icon'
                onClick={() => removeIngredient(index)}
                className='text-red-500 hover:bg-red-50 hover:text-red-700'
              >
                <Trash2 className='size-4' />
              </Button>
            )}
          </div>
        ))}
        {errors.ingredients && <p className='text-sm text-red-500'>{errors.ingredients.message}</p>}
      </div>

      <div className='relative py-8'>
        <div
          className='absolute inset-0 flex items-center'
          aria-hidden='true'
        >
          <div className='w-full border-t border-gray-200'></div>
        </div>
        <div className='relative flex justify-center'>
          <span className='bg-white px-6 text-sm font-medium text-gray-500'>Time to Work Your Magic!</span>
        </div>
      </div>

      <div className='space-y-4'>
        <div className='flex items-center justify-between'>
          <Label>Steps</Label>
          <Button
            type='button'
            variant='outline'
            size='sm'
            onClick={addStep}
            className='border-[#FF7C75] text-[#FF7C75] hover:bg-[#FF7C75] hover:text-white'
          >
            <Plus className='mr-2 size-4' />
            Add Step
          </Button>
        </div>
        {steps?.map((_, index) => (
          <div
            key={index}
            className='flex items-start gap-4'
          >
            <div className='h-[120px] w-1/4'>
              <div
                onClick={() => handleStepImageClick(index)}
                className='relative h-full cursor-pointer rounded-lg border-2 border-dashed border-[#FF7C75] p-2 transition-colors hover:border-[#FF7C75]/80'
              >
                <input
                  type='file'
                  ref={(el) => {
                    stepImageRefs.current[index] = el;
                  }}
                  onChange={handleStepImageChange(index)}
                  accept='image/*'
                  className='hidden'
                />
                {stepImages[index] ? (
                  <div className='relative h-full'>
                    <img
                      src={stepImages[index]!}
                      alt={`Step ${index + 1}`}
                      className='size-full rounded-lg object-cover'
                    />
                    <Button
                      type='button'
                      variant='ghost'
                      size='icon'
                      className='absolute left-2 top-2 bg-white/90 text-red-500 hover:bg-white hover:text-red-700'
                      onClick={handleStepImageRemove(index)}
                    >
                      <Trash2 className='size-4' />
                    </Button>
                  </div>
                ) : (
                  <div className='flex h-full flex-col items-center justify-center text-gray-500'>
                    <ImageIcon className='mb-1 size-8' />
                    <p className='text-sm font-medium'>Show us how!</p>
                    <p className='text-xs text-gray-400'>Make it visual</p>
                  </div>
                )}
              </div>
            </div>
            <div className='flex-1'>
              <Textarea
                {...register(`steps.${index}.details`)}
                placeholder={`Step ${index + 1}: ${getPlaceholder(shuffledSteps, index)}`}
                className='min-h-[120px] border-[#FF7C75] focus:border-[#FF7C75] focus:ring-[#FF7C75]'
              />
            </div>
            {steps.length > 1 && (
              <Button
                type='button'
                variant='ghost'
                size='icon'
                onClick={() => removeStep(index)}
                className='text-red-500 hover:bg-red-50 hover:text-red-700'
              >
                <Trash2 className='size-4' />
              </Button>
            )}
          </div>
        ))}
        {errors.steps && <p className='text-sm text-red-500'>{errors.steps.message}</p>}
      </div>

      {/* Commit Message Field - Only show in edit mode when there are details changes */}
      {showCommitMessage && (
        <div className='mt-6 rounded-lg border border-[#FF7C75]/20 bg-[#FF7C75]/10 p-4'>
          <label className='mb-2 block text-sm font-medium text-[#FF7C75]'>
            Commit Message (required for recipe detail changes)
          </label>
          <input
            type='text'
            value={commitMessage}
            onChange={(e) => onCommitMessageChange?.(e.target.value)}
            placeholder='Describe the changes you made...'
            className='w-full rounded-md border border-[#FF7C75]/30 px-3 py-2 focus:border-[#FF7C75] focus:outline-none focus:ring-2 focus:ring-[#FF7C75]'
          />
        </div>
      )}

      <Button
        type='submit'
        disabled={isSubmitting || disabled}
        className='w-full bg-[#FF7C75] py-6 text-lg font-semibold text-white hover:bg-[#FF7C75]/90 disabled:cursor-not-allowed disabled:border-gray-300 disabled:bg-gray-400 disabled:text-gray-500'
      >
        {isSubmitting ? 'Saving...' : submitButtonText}
      </Button>
    </form>
  );
}
