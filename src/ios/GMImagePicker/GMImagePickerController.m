//
//  GMImagePickerController.m
//  GMPhotoPicker
//
//  Created by Guillermo Muntaner Perelló on 19/09/14.
//  Copyright (c) 2014 Guillermo Muntaner Perelló. All rights reserved.
//

#import "GMImagePickerController.h"
#import "GMAlbumsViewController.h"
#import "GMFetchItem.h"
#import "GMGridViewCell.h"

@interface GMImagePickerController () <UINavigationControllerDelegate,UIScrollViewDelegate>

@end

@implementation GMImagePickerController

- (id)init:(bool)allow_v
{
    if (self = [super init])
    {
        _allow_video = allow_v;
        
        _selectedAssets = [[NSMutableArray alloc] init];
        _selectedFetches = [[NSMutableArray alloc] init];
        
        //Default values:
        _displaySelectionInfoToolbar = YES;
        _displayAlbumsNumberOfAssets = YES;
        
        //Grid configuration:
        _colsInPortrait = 3;
        _colsInLandscape = 5;
        _minimumInteritemSpacing = 2.0;
        
        //Sample of how to select the collections you want to display:
        _customSmartCollections = @[@(PHAssetCollectionSubtypeSmartAlbumFavorites),
                                    @(PHAssetCollectionSubtypeSmartAlbumRecentlyAdded),
                                    @(PHAssetCollectionSubtypeSmartAlbumVideos),
                                    @(PHAssetCollectionSubtypeSmartAlbumSlomoVideos),
                                    @(PHAssetCollectionSubtypeSmartAlbumTimelapses),
                                    @(PHAssetCollectionSubtypeSmartAlbumBursts),
                                    @(PHAssetCollectionSubtypeSmartAlbumPanoramas)];
        //If you don't want to show smart collections, just put _customSmartCollections to nil;
        //_customSmartCollections=nil;
        
        self.preferredContentSize = kPopoverContentSize;
        
        [self setupNavigationController];
    }
    return self;
}

- (void)dealloc
{
    NSLog(@"dealloc");
    _selectedCell = nil;
}


- (void)viewDidLoad
{
    NSNotificationCenter *notiCenter = [NSNotificationCenter defaultCenter];
    [notiCenter addObserver:self selector:@selector(notificationEvent:) name:@"mycell" object:nil];
    [notiCenter addObserver:self selector:@selector(cancleCellEvent:) name:@"cancleCell" object:nil];
    _selectedCell = [[NSMutableArray alloc]init];
    _previewbtn = [[UIButton alloc]init];
    _preVC = [[UIViewController alloc]init];
    _tocoMpletebtn = [[UIButton alloc]init];
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}
-(void)viewWillAppear:(BOOL)animated
{
    NSLog(@"视图将要加载。。。");
}
-(void)cancleCellEvent:(NSNotification *)notification{
   
    _cancleSelectedCells = [[NSArray alloc]init];
    _cancleSelectedCells = notification.object;
}

- (void)notificationEvent:(NSNotification *)notification{
    _indexpath = [[NSIndexPath alloc]init];
    _arr = [[NSArray alloc]init];
    _arr = notification.object;

}
- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Setup Navigation Controller

- (void)setupNavigationController
{
    
    GMAlbumsViewController *albumsViewController = [[GMAlbumsViewController alloc] init:_allow_video];
    _navigationController = [[UINavigationController alloc] initWithRootViewController:albumsViewController];
    _navigationController.delegate = self;
   
    
    [_navigationController willMoveToParentViewController:self];
    [_navigationController.view setFrame:self.view.frame];
   
    [self.view addSubview:_navigationController.view];
    [self addChildViewController:_navigationController];
    [_navigationController didMoveToParentViewController:self];
}

#pragma mark - Select / Deselect Asset

- (void)selectAsset:(PHAsset *)asset
{
    
 
  
    GMGridViewCell *mycell = (GMGridViewCell *)[_arr[0] cellForItemAtIndexPath:_arr[1]];

    NSDictionary *dict = [[NSDictionary alloc]initWithObjectsAndKeys:mycell,_arr[1] ,nil];
    [_selectedCell addObject:dict];
    
    [mycell.selectedButton setTitle:[NSString stringWithFormat:@"%ld",(unsigned long)self.selectedAssets.count + 1] forState:(UIControlStateNormal)];
    [self.selectedAssets insertObject:asset atIndex:self.selectedAssets.count];
    [self updateDoneButton];
    
    if(self.displaySelectionInfoToolbar)
        [self updateToolbar];
    
}

- (void)deselectAsset:(PHAsset *)asset
{
   
    
    
    GMGridViewCell *mycell = (GMGridViewCell *)[_cancleSelectedCells[0] cellForItemAtIndexPath:_cancleSelectedCells[1]];
    
    for(int i = 0;i<_selectedCell.count;i++)
    {
        NSDictionary *dict = _selectedCell[i];
        
        if(mycell == [dict objectForKey:dict.allKeys[0]])
        {
            [_selectedCell removeObjectAtIndex:i];
            for (int j = 0;j<_selectedCell.count;j++)
            {
                NSDictionary *surplusDict = _selectedCell[j];
                
                
                GMGridViewCell *surplusCell = (GMGridViewCell *)[_arr[0] cellForItemAtIndexPath:surplusDict.allKeys[0]];
                [surplusCell.selectedButton setTitle:[NSString stringWithFormat:@"%ld",(unsigned long)j + 1] forState:(UIControlStateNormal)];
               
            
            }
         
        }

    }
    

[self.selectedAssets removeObjectAtIndex:[self.selectedAssets indexOfObject:asset]];
    if(self.selectedAssets.count == 0)
        [self updateDoneButton];
    
    if(self.displaySelectionInfoToolbar)
        [self updateToolbar];
     
}

- (void)selectFetchItem:(GMFetchItem *)fetch_item{
    [self.selectedFetches insertObject:fetch_item atIndex:self.selectedFetches.count];
}

- (void)deselectFetchItem:(GMFetchItem *)fetch_item{
    [self.selectedFetches removeObjectAtIndex:[self.selectedFetches indexOfObject:fetch_item]];
}


- (void)updateDoneButton
{
    UINavigationController *nav = (UINavigationController *)self.childViewControllers[0];
    for (UIViewController *viewController in nav.viewControllers)
//        viewController.navigationItem.rightBarButtonItem.enabled = (self.selectedAssets.count > 0);
    viewController.navigationItem.rightBarButtonItem.enabled = YES;
}

- (void)updateToolbar
{
        UINavigationController *nav = (UINavigationController *)self.childViewControllers[0];
    for (UIViewController *viewController in nav.viewControllers)
    {
        [[viewController.toolbarItems objectAtIndex:1] setTitle:[self toolbarTitle]];
        
        
     

        [viewController.navigationController setToolbarHidden:(self.selectedAssets.count == 0) animated:YES];
    }
    

    _previewbtn.frame =CGRectMake(0, self.view.frame.size.height-45, 100, 45);

    
    [_previewbtn setTitle:@"预览" forState:(UIControlStateNormal)];
    [_previewbtn setTitleColor:[UIColor colorWithRed:63.0/255.0 green:159.0/255.0 blue:1.0 alpha:1.0] forState:(UIControlStateNormal)];
     _previewbtn.titleLabel.font = [UIFont boldSystemFontOfSize:20];
    _previewbtn.titleLabel.adjustsFontSizeToFitWidth = YES;
    [_previewbtn.titleLabel setTextAlignment:(NSTextAlignmentLeft)];
   
    [_previewbtn addTarget:self action:@selector(previewEvent:) forControlEvents:(UIControlEventTouchUpInside)];
        [_previewbtn setHidden:(self.selectedAssets.count == 0)];
    NSLog(@"(self.selectedAssets.count == 0):%lu",(unsigned long)self.selectedAssets.count);
    
    [nav.view addSubview:_previewbtn];
    
    
    _tocoMpletebtn.frame =CGRectMake(self.view.frame.size.width - 60, self.view.frame.size.height-45, 60, 45);
    [_tocoMpletebtn setTitle:@"完成" forState:(UIControlStateNormal)];
    [_tocoMpletebtn setTitleColor:[UIColor colorWithRed:63.0/255.0 green:159.0/255.0 blue:1.0 alpha:1.0] forState:(UIControlStateNormal)];
    _tocoMpletebtn.titleLabel.font = [UIFont boldSystemFontOfSize:20];
    _tocoMpletebtn.titleLabel.adjustsFontSizeToFitWidth = YES;
    [_tocoMpletebtn.titleLabel setTextAlignment:(NSTextAlignmentLeft)];
    
    [_tocoMpletebtn addTarget:self action:@selector(finishPickingAssets:) forControlEvents:(UIControlEventTouchUpInside)];
 
   [_tocoMpletebtn setHidden:(self.selectedAssets.count == 0)];
    
    [nav.view addSubview:_tocoMpletebtn];
    
    
    

}
#pragma mark -preview
- (void)previewEvent:(UIButton *)sender
{
    
//    [sender removeFromSuperview];
    [sender setHidden:YES];
    
    UIScrollView *previewScroll = [[UIScrollView alloc]initWithFrame:CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height)];
    int i=0;
    for(GMFetchItem *item in self.selectedFetches){
        UIImageView *iv = [[UIImageView alloc]initWithFrame:CGRectMake(i*self.view.frame.size.width + 2, 2, self.view.frame.size.width-4, self.view.frame.size.height-4)];
        [iv setImage:[UIImage imageNamed:item.image_fullsize]];
        [previewScroll addSubview:iv];
        i++;
        
    }

    
    NSInteger icont = self.selectedFetches.count;
    //设定可滚动范围
    previewScroll.contentSize=CGSizeMake(icont*self.view.frame.size.width, self.view.frame.size.width);
    //设定当前显示的偏移
    previewScroll.contentOffset=CGPointMake(0, 0);
    previewScroll.pagingEnabled=YES;//页效果（只一个一个了视图的显示）
    previewScroll.bounces=YES;
    previewScroll.delegate=self;
    
   
   
    _preVC.view.backgroundColor = [UIColor whiteColor];
    _preVC.navigationItem.title = [NSString stringWithFormat:@"1/%lu",(unsigned long)self.selectedFetches.count];
//    UIBarButtonItem *barBtn = [[UIBarButtonItem alloc]initWithBarButtonSystemItem:(UIBarButtonSystemItemCompose) target:self action:@selector(back)];
    UIButton *backbtn = [[UIButton alloc]initWithFrame:CGRectMake(0, 9, 20, 25)];
    [backbtn setBackgroundImage:[UIImage imageNamed:@"imageback"] forState:(UIControlStateNormal)];
   
   
  
    [backbtn addTarget:self action:@selector(back) forControlEvents:(UIControlEventTouchUpInside)];
    UIBarButtonItem *barBtn = [[UIBarButtonItem alloc]initWithCustomView:backbtn];
    
    _preVC.navigationItem.leftBarButtonItem = barBtn;
    
    
    _preVC.navigationItem.rightBarButtonItem =
    [[UIBarButtonItem alloc] initWithTitle:NSLocalizedStringFromTable(@"取消", @"GMImagePicker",@"Done")
                                     style:UIBarButtonItemStyleDone
                                    target:self
                                    action:@selector(dismiss:)];
    
   
    _preVC.navigationItem.rightBarButtonItem.enabled = YES;
  
    
    [_preVC.view addSubview:previewScroll];
    [self.navigationController pushViewController:_preVC animated:YES];

    
}



-(void)back{
    
    
    [_previewbtn setHidden:NO];
    [self.navigationController popViewControllerAnimated:YES];
}


#pragma mark -Scroll Delegate
-(void)scrollViewWillEndDragging:(UIScrollView *)scrollView withVelocity:(CGPoint)velocity targetContentOffset:(inout CGPoint *)targetContentOffset
{
    
    NSInteger i =(int)targetContentOffset->x/self.view.frame.size.width;
    _preVC.navigationItem.title = [NSString stringWithFormat:@"%lu/%lu",(long)i+1,(unsigned long)self.selectedFetches.count];
}

#pragma mark - User finish Actions

- (void)dismiss:(id)sender
{
    if ([self.delegate respondsToSelector:@selector(assetsPickerControllerDidCancel:)])
        [self.delegate assetsPickerControllerDidCancel:self];
    
    [self.presentingViewController dismissViewControllerAnimated:YES completion:nil];
}


- (void)finishPickingAssets:(id)sender
{
    if ([self.delegate respondsToSelector:@selector(assetsPickerController:didFinishPickingAssets:)])
        //[self.delegate assetsPickerController:self didFinishPickingAssets:self.selectedAssets];
        [self.delegate assetsPickerController:self didFinishPickingAssets:self.selectedFetches];
    
    //[self.presentingViewController dismissViewControllerAnimated:YES completion:nil];
}


#pragma mark - Toolbar Title

- (NSPredicate *)predicateOfAssetType:(PHAssetMediaType)type
{
    return [NSPredicate predicateWithBlock:^BOOL(PHAsset *asset, NSDictionary *bindings) {
        return (asset.mediaType==type);
    }];
}

- (NSString *)toolbarTitle
{
    if (self.selectedAssets.count == 0)
        return nil;
    
    NSPredicate *photoPredicate = [self predicateOfAssetType:PHAssetMediaTypeImage];
    NSPredicate *videoPredicate = [self predicateOfAssetType:PHAssetMediaTypeVideo];
    
    NSInteger nImages = [self.selectedAssets filteredArrayUsingPredicate:photoPredicate].count;
    NSInteger nVideos = [self.selectedAssets filteredArrayUsingPredicate:videoPredicate].count;
    
    if (nImages>0 && nVideos>0)
    {
        return [NSString stringWithFormat:NSLocalizedStringFromTable(@"选取多张图片", @"GMImagePicker", @"%@ Items Selected" ), @(nImages+nVideos)];
    }
    else if (nImages>1)
    {
        return [NSString stringWithFormat:NSLocalizedStringFromTable(@"选取多张图片", @"GMImagePicker", @"%@ Photos Selected"), @(nImages)];
    }
    else if (nImages==1)
    {
        return NSLocalizedStringFromTable(@"选取单张图片", @"GMImagePicker", @"1 Photo Selected" );
    }
    else if (nVideos>1)
    {
        return [NSString stringWithFormat:NSLocalizedStringFromTable(@"picker.selection.multiple-videos", @"GMImagePicker", @"%@ Videos Selected"), @(nVideos)];
    }
    else if (nVideos==1)
    {
        return NSLocalizedStringFromTable(@"picker.selection.single-video", @"GMImagePicker", @"1 Video Selected");
    }
    else
    {
        return nil;
    }
}


#pragma mark - Toolbar Items

- (UIBarButtonItem *)titleButtonItem
{
    UIBarButtonItem *title =
    [[UIBarButtonItem alloc] initWithTitle:self.toolbarTitle
                                     style:UIBarButtonItemStylePlain
                                    target:nil
                                    action:nil];
    
    NSDictionary *attributes = @{NSForegroundColorAttributeName : [UIColor blackColor]};
    
    [title setTitleTextAttributes:attributes forState:UIControlStateNormal];
    [title setTitleTextAttributes:attributes forState:UIControlStateDisabled];
    [title setEnabled:NO];
    
    return title;
}

- (UIBarButtonItem *)spaceButtonItem
{
    return [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
}

- (NSArray *)toolbarItems
{
    UIBarButtonItem *title = [self titleButtonItem];
    UIBarButtonItem *space = [self spaceButtonItem];
    
    return @[space, title, space];
}



@end
