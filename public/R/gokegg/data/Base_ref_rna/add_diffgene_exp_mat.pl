#!usr/bin/perl -w

use strict;

die "usage:perl $0 <diff gene> <gene fpkm matrix> <diff.comb.txt>\n" unless @ARGV == 3;

open A,$ARGV[0];open B,$ARGV[1];	open C,$ARGV[2];
my %diff;	my %select1;	my %select2;

while(<C>){
	chomp;	my @a=split;
	my @b1=split /,/,$a[1];	my @b2=split /,/,$a[2];
	foreach my $k(@b1){		$select1{$a[0]}{$k}=1;	}
	foreach my $k(@b2){             $select2{$a[0]}{$k}=1;  }
}
close C;my $id="a";
while(<A>){
	chomp;my @a=split;	    $diff{$a[0]}=1;
	$id="$a[2],$a[3]";
}
close A;

my $head=<B>;	chomp $head;	my @head=split /\s+/,$head;
my @part_head1;	my @part_head2;	my %site1;	my %site2;
foreach my $k(1..$#head){
	if(exists $select1{$id}{$head[$k]}){
		push @part_head1,$head[$k];
		$site1{$k}=1;
	}
}
foreach my $k(1..$#head){
	if(exists $select2{$id}{$head[$k]}){
		push @part_head2,$head[$k];
		$site2{$k}=1;
	}
}
my $part_head1=join "\t",@part_head1;		my $part_head2=join "\t",@part_head2;
print "$head[0]\t$part_head1\t$part_head2\n";
while(<B>){
	chomp;my @a=split;
	if(exists $diff{$a[0]}){
		print "$a[0]";
		foreach my $k(1..$#a){
			if(exists $site1{$k}){				print "\t$a[$k]";			}
		}
		foreach my $k(1..$#a){
			if(exists $site2{$k}){				print "\t$a[$k]";			}
		}
		print "\n";
	}
}
close B;
